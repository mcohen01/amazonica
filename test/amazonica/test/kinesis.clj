(ns amazonica.test.kinesis
  (:import org.joda.time.DateTime
           java.nio.ByteBuffer
           java.util.UUID
           com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker)
  (:require [clojure.string :as string]
            [amazonica.aws.dynamodbv2 :as dyna])
  (:use [clojure.test]
        [amazonica.aws.kinesis]))

(def ^:private table "KinesisTestTable")

(def ^:private dynamodb-stream-timeout 60) ; seconds

(def cred
  (let [file  (str (System/getProperty "user.home") "/.aws/credentials")
        lines (string/split (slurp file) #"\n")
        creds (into {} (filter second (map #(string/split % #"\s*=\s*") lines)))]
    {:access-key (get creds "aws_access_key_id")
     :secret-key (get creds "aws_secret_access_key")}))

(deftest kinesis

  (def ^String my-stream "my-stream")
  (def ^DateTime now (DateTime.))

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

  (defn get-raw-bytes [^java.nio.ByteBuffer byte-buffer]
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
    ^bytes (:data)
    (String.)
    (= "foobar")
    (is))

  (delete-stream my-stream)

  )


(def ^:private streamed-data (atom []))

(defn- deserializer [^java.nio.ByteBuffer byte-buffer]
  (try
    (let [bytes (byte-array (.remaining byte-buffer))]
      (.get byte-buffer bytes)
      (String. bytes "UTF-8"))
    (catch Exception e
      (println e )
      (is false))))

(defn- processor! [records]
  (try
    (swap! streamed-data concat (map :data records))
    true
    (catch Exception e
      (println e)
      (is false)
      false)))

(def ^:private expected-data "\"NewImage\":{\"name\":{\"S\":\"example\"},\"id\":{\"S\":\"1\"}}")

(defn- get-region [stream-arn]
  (second (re-matches #"^arn:aws:dynamodb:(.+?):.*$" stream-arn)))

(deftest kinesis-dynamodb-streams
  (reset! streamed-data [])

  (try
    (dyna/create-table
      :table-name table
      :key-schema
      [{:attribute-name "id" :key-type "HASH"}]
      :attribute-definitions
      [{:attribute-name "id" :attribute-type "S"}]
      :provisioned-throughput
      {:read-capacity-units  1
       :write-capacity-units 1}
      :stream-specification {:stream-enabled true
                              :stream-view-type "NEW_IMAGE"})

    ; wait for the tables to be created
    (doseq [table (:table-names (dyna/list-tables))]
      (loop [status (get-in (dyna/describe-table :table-name table)
                            [:table :table-status])]
        (if-not (= "ACTIVE" status)
          (do
            (println "waiting for status" status "to be active")
            (Thread/sleep 1000)
            (recur (get-in (dyna/describe-table :table-name table)
                           [:table :table-status]))))))

    (let [stream-arn (get-in (dyna/describe-table :table-name table) [:table :latest-stream-arn])
          [^Worker w _] (worker :app                        (str (UUID/randomUUID))
                                :dynamodb-adaptor-client?   true
                                :stream                     stream-arn
                                :endpoint                   (str "streams.dynamodb." (get-region stream-arn) ".amazonaws.com")
                                :initial-position-in-stream "TRIM_HORIZON"
                                :deserializer               deserializer
                                :processor                  processor!)]
      (future
        (try
          (.run w)
          (catch Exception e
            (println e)
            (is false))))

      (dyna/put-item
        :table-name table
        :item {:id "1" :name "example"})

      (try
        (loop [c 0]
          (println "Waiting for streamed data to arrive " c " seconds")
          (Thread/sleep 1000)
          (when (and (empty? @streamed-data)
                     (< c dynamodb-stream-timeout))
            (recur (inc c))))

        (is (= 1 (count @streamed-data)))
        (is (.contains ^String (first @streamed-data) expected-data))

        (finally
          (.shutdown w))))

    (finally
      (dyna/delete-table :table-name table))))


