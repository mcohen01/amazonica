(ns amazonica.test.kinesis
  (:import org.joda.time.DateTime
           java.nio.ByteBuffer
           java.util.UUID)
  (:use [clojure.test]
        [amazonica.aws.kinesis]))

(deftest kinesis []

  (def my-stream "my-stream")
  (def now (DateTime.))
  
  (create-stream my-stream 1)
  
  (list-streams)
  
  ; wait for the stream to be created
  (loop [status (get-in (describe-stream my-stream)
                        [:stream-description :stream-status])]
    (if-not (= "ACTIVE" status)
      (do 
        (println "waiting for status" status "to be active")
        (Thread/sleep 1000)
        (recur (get-in (describe-stream my-stream)
                        [:stream-description :stream-status])))))
  
  ;(split-shard my-stream "shardId-000000000000" "2")
  
  ;(merge-shards my-stream "shardId-000000000001" "shardId-000000000002")  
  
  (let [data {:name "any data"
              :col  #{"anything" "at" "all"}
              :date now}
        sn (:sequence-number (put-record my-stream data (str (UUID/randomUUID))))]
    (put-record my-stream data (str (UUID/randomUUID)) sn))
  
  (Thread/sleep 3000)
  
  (def shard (-> (describe-stream my-stream)
                 :stream-description
                 :shards
                 last
                 :shard-id))
  
  (let [iter  (get-shard-iterator my-stream shard "TRIM_HORIZON")
        resp  (get-records :shard-iterator iter)
        rows  (:records resp)]
    (is (= #{"anything" "at" "all"}
           (-> rows first :data :col)))
    (is (= "any data"
           (-> rows first :data :name)))
    (is (.equals now (-> rows first :data :date))))  
  
  (def seq-number
    (:sequence-number (put-record my-stream
                                  (ByteBuffer/wrap (.getBytes "foobar"))
                                  (str (UUID/randomUUID)))))
  
  (defn get-raw-bytes [byte-buffer]
    (let [b (byte-array (.remaining byte-buffer))]
      (.get byte-buffer b)
      b))
  
  (Thread/sleep 3000)
  
  (-> (get-records :deserializer get-raw-bytes
                   :shard-iterator 
                   (get-shard-iterator my-stream
                                       shard
                                       "AT_SEQUENCE_NUMBER"
                                       seq-number))
      :records
      first
      :data
      (String.)
      (= "foobar")
      (is))
  
  (delete-stream my-stream)
  
)