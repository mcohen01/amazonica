(ns amazonica.test.kinesis
  (:import org.joda.time.DateTime
           java.nio.ByteBuffer
           java.util.UUID)
  (:require [clojure.string :as string])
  (:use [clojure.test]
        [amazonica.aws.kinesis]))

(def cred
  (let [file  (str (System/getProperty "user.home") "/.aws/credentials")
        lines (string/split (slurp file) #"\n")
        creds (into {} (filter second (map #(string/split % #"\s*=\s*") lines)))]
    {:access-key (get creds "aws_access_key_id")
     :secret-key (get creds "aws_secret_access_key")}))

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

  ;; test invocations with and without credentials map passed in
  ;; test invocations passed with key:value arg pairs and with a single map
  (->> (get-shard-iterator my-stream shard "TRIM_HORIZON")
    (hash-map :shard-iterator)
    (get-records))

  (->> (get-shard-iterator my-stream shard "TRIM_HORIZON")
    (hash-map :shard-iterator)
    (get-records cred))

  (->> (get-shard-iterator my-stream shard "TRIM_HORIZON")
    (get-records :shard-iterator))

  (->> (get-shard-iterator my-stream shard "TRIM_HORIZON")
    (get-records cred :shard-iterator))


  (let [iter  (get-shard-iterator my-stream shard "TRIM_HORIZON")
        resp  (get-records :shard-iterator iter)
        rows  (:records resp)]
    (is (= #{"anything" "at" "all"}
           (-> rows first :data :col)))
    (is (= "any data"
           (-> rows first :data :name)))
    (is (.equals now (-> rows first :data :date))))



  ;; test unmarshalled "raw" data, no nippy serialization/deserialization
  (def seq-number
    (:sequence-number (put-record my-stream
                                  (ByteBuffer/wrap (.getBytes "foobar"))
                                  (str (UUID/randomUUID)))))

  (defn get-raw-bytes [byte-buffer]
    (let [b (byte-array (.remaining byte-buffer))]
      (.get byte-buffer b)
      b))

  (Thread/sleep 3000)

  (-> (get-records {:deserializer get-raw-bytes
                    :shard-iterator
                    (get-shard-iterator my-stream
                                        shard
                                        "AT_SEQUENCE_NUMBER"
                                        seq-number)})
    :records
    first
    :data
    (String.)
    (= "foobar")
    (is))

  (delete-stream my-stream)

  )
