(ns amazonica.test.kinesisasync
  (:use [clojure.test]
        [amazonica.aws.kinesis :as kinesis]
        [taoensso.nippy :as nippy]
        [clojure.core.async :refer [chan close! <!! >!! sliding-buffer]])
  (:import [com.amazonaws.services.kinesis.clientlibrary.interfaces
              IRecordProcessor
              IRecordProcessorFactory
              IRecordProcessorCheckpointer]
           [com.amazonaws.services.kinesis.model
            Record]
           [com.amazonaws.services.kinesis.clientlibrary.exceptions
              InvalidStateException
              KinesisClientLibDependencyException
              ShutdownException
              ThrottlingException]
           [com.amazonaws.services.kinesis.clientlibrary.types
            ShutdownReason]
           java.nio.ByteBuffer))

; com.amazonaws.services.kinesis.model Record
(defn mockRecord [seq data]
  (proxy [Record] []
    (getSequenceNumber []
      (str seq))
    (getPartitionKey []
      nil)
    (getData [] 
      (java.nio.ByteBuffer/wrap (nippy/freeze data )))))

; While Kinesis would store checkpointed sequence numbers in DynamoDB, 
; the mock stores this state in atom cp-state so it can be observed.
(defn mockCheckpointer [cp-state]
  (reify IRecordProcessorCheckpointer
    (checkpoint [this]
      (println "Checkpointing"))
    (checkpoint [this s]   ; void checkpoint(String sequenceNumber)
      (reset! cp-state s)
      )))

; Simulates a Kinesis ThrottlingException on the first call. A subsequent call will 
; checkpoint successfully.
(defn mockCheckpointer-throttled [cp-state]
  (let [call-count (atom 0)]
    (reify IRecordProcessorCheckpointer
      (checkpoint [this]
        (println "Checkpointing"))
      (checkpoint [this s]   ; void checkpoint(String sequenceNumber)
        (if (zero? @call-count)
          (do
            (swap! call-count inc)
            ;(println "Exception thrown")
            (throw (ThrottlingException. "simulated throttle")))
          (do (reset! call-count 0)
            (reset! cp-state s)))))))

(deftest async-sanity-check []
  (let [shard-chan (chan 10)                ; buffered shard channel
        cp-chan (chan (sliding-buffer 1))   ; checkpoint channel
        next-check (atom 0)                 ; time of next checkpoint (ms)
        cp-interval 0.000001                ; time between checkpoint intervals (seconds)
        cp-state (atom -1)                  ; value of latest checkpointed sequence number
        factory (#'kinesis/processor-factory-async shard-chan cp-chan unwrap cp-interval next-check)
        processor (.createProcessor factory)
        cp-mock (mockCheckpointer cp-state)
        cp-mock-throttled (mockCheckpointer-throttled cp-state)]
    
        ; Simulate app checkpoint of sequence number 0
        (>!! cp-chan 0)
        ; Simulate incoming data from Kinesis, which will be written to shard-chan
        (let [records (java.util.ArrayList. [(mockRecord 1 "data1") (mockRecord 2 2) (mockRecord 3 [3 6 9])]) ]
          (.processRecords processor records cp-mock-throttled))
        ; Verify sequence number 0 was checkpointed as a side effect of processRecords
        (is (= @cp-state "0"))
        ; Simulate app reading from the buffered shard channel
        (let [d1 (<!! shard-chan)
              d2 (<!! shard-chan)
              d3 (<!! shard-chan)]
              ; Verify the sequence numbers and data received on the channel
              (is (= (:sequence-number d1) "1"))
              (is (= (:data d1) "data1"))
              (is (= (:sequence-number d2) "2"))
              (is (= (:data d2) 2))
              (is (= (:sequence-number d3) "3"))
              (is (= (:data d3) [3 6 9])))
        ; Simulate completion of processing by writing sequence #3 to the checkpoint channel
        (>!! cp-chan 3)
        ; Simulate termination of processing initiated by Kinesis
        (.shutdown processor cp-mock ShutdownReason/TERMINATE)
        ; Verify sequence number 3 was checkpointed as a side effect of termination
        (is (= @cp-state "3"))
    ))

