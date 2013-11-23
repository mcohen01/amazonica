(ns amazonica.test.kinesis
  (:import org.joda.time.DateTime
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
  
  ;; (merge-shards my-stream "shardId-000000000000" "shardId-000000000001")
  
  ;; (split-shard my-stream "shard-already_pathid" "new-starting-hash-key")
  
  
  (let [data {:name "any data"
              :col  #{"anything" "at" "all"}
              :date now}]
    (put-record my-stream
                data
                (str (UUID/randomUUID))))
  
  (Thread/sleep 3000)
  
  (let [shard (-> (describe-stream my-stream)
                  :stream-description
                  :shards
                  first
                  :shard-id)
        iter  (get-shard-iterator my-stream shard "TRIM_HORIZON")
        resp  (get-next-records iter)
        rows  (:records resp)]
    (is (= #{"anything" "at" "all"}
           (-> rows first :data :col)))
    (is (= "any data"
           (-> rows first :data :name)))
    (is (.equals now (-> rows first :data :date))))
  
  (delete-stream my-stream)
  
)