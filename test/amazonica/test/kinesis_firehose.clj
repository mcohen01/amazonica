(ns amazonica.test.kinesis-firehose
  ;; (:import org.joda.time.DateTime java.nio.ByteBuffer java.util.UUID)
  (:require [amazonica.aws.kinesisfirehose :as fh]
            [amazonica.aws.s3 :as s3])
  (:require [clojure.test :refer :all]
            [clojure.string :as string]))

(def stream-prefix "amazonica-kinesis-firehose-tests-ca232885fa3e-")

(def cred
  (let [file  (str (System/getProperty "user.home") "/.aws/credentials")
        lines (string/split (slurp file) #"\n")
        creds (into {} (filter second (map #(string/split % #"\s*=\s*") lines)))]
    {:access-key (get creds "aws_access_key_id")
     :secret-key (get creds "aws_secret_access_key")}))

(defn list-delivery-streams-matching-prefix []
  (->> (fh/list-delivery-streams cred)
       :delivery-stream-names
       (filter #(.startsWith % stream-prefix))))

(defn delete-delivery-streams-matching-prefix []
  (->> (list-delivery-streams-matching-prefix)
       (map #(fh/delete-delivery-stream cred :delivery-stream-name %))
       doall))

(defn wait-for-stream-to-change-status [cred stream-name status]
  (loop [c 0]
    (let [st (fh/describe-delivery-stream cred :delivery-stream-name stream-name)]
      (when (> c 100)
        (throw (ex-data "Timeout waiting for stream status change" {:stream-name stream-name :status status :response st})))
      (when (-> st :delivery-stream-description :delivery-stream-status (not= status))
        (Thread/sleep 1000)
        (recur (inc c))))))

(deftest firehose
  (testing "Kinesis Firehose"
    (let [list-of-streams (testing "get list delivery streams" (fh/list-delivery-streams cred 10000))]
      (testing "list delivery streams"
        (is (every? string? (:delivery-stream-names list-of-streams)))
        (testing "returns whole list when there's too many for the page size"
          (binding [fh/*list-delivery-streams-default-limit* 1]
            (is (= list-of-streams (fh/list-delivery-streams cred))))))

      (testing "delete delivery streams where the name matches the prefix"
        (delete-delivery-streams-matching-prefix)
        (is (empty? (list-delivery-streams-matching-prefix))))

      (let [stream-name (str stream-prefix "--test-stream")
            new-bucket-name (str stream-name 2)]
        (try
          (testing "create delivery stream"
            (s3/create-bucket cred stream-name)
            (fh/create-delivery-stream cred {:delivery-stream-name stream-name
                                             :S3DestinationConfiguration {:BucketARN (str "arn:aws:s3:::" stream-name)
                                                                          :BufferingHints {:IntervalInSeconds 300 :SizeInMBs 5}
                                                                          :CompressionFormat "UNCOMPRESSED"
                                                                          :EncryptionConfiguration {:NoEncryptionConfig "NoEncryption"}
                                                                          :Prefix "string"
                                                                          :RoleARN "arn:aws:iam::909704556315:role/firehose_delivery_role"}})
            (testing "describe delivery stream"
              (wait-for-stream-to-change-status cred stream-name "ACTIVE"))
            (is (some #{stream-name} (:delivery-stream-names (fh/list-delivery-streams cred))))
            (testing "put-record"
              (is (string? (:record-id (fh/put-record cred stream-name [1,2,3,4]))))
              (is (string? (:record-id (fh/put-record cred {:record {:data "1234"} :delivery-stream-name stream-name}))))
              (is (string? (:record-id (fh/put-record cred :record {:data "1234"} :delivery-stream-name stream-name)))))
            (testing "put-record-batch"
              (are [resp] (and (= 0 (:failed-put-count resp))
                               (every? string? (map :record-id (:request-responses resp))))
                (fh/put-record-batch cred stream-name [[1 2 3 4] ["test" 2 3 4] "\"test\",2,3,4"])
                (fh/put-record-batch cred {:records [{:data "some data"} {:data "some more data"}] :delivery-stream-name stream-name})
                (fh/put-record-batch cred :records [{:data "some data"} {:data "some more data"}] :delivery-stream-name stream-name))))
          (testing "update destination"
            (let [{{:keys [version-id] [{:keys [destination-id]}] :destinations} :delivery-stream-description} (fh/describe-delivery-stream cred :delivery-stream-name stream-name)]
              (s3/create-bucket cred new-bucket-name)
              (fh/update-destination cred {:CurrentDeliveryStreamVersionId version-id
                                           :DeliveryStreamName stream-name
                                           :DestinationId destination-id
                                           :S3DestinationUpdate {:BucketARN (str "arn:aws:s3:::" new-bucket-name)
                                                                 :BufferingHints {:IntervalInSeconds 300
                                                                                  :SizeInMBs 5}
                                                                 :CompressionFormat "UNCOMPRESSED"
                                                                 :EncryptionConfiguration {:NoEncryptionConfig "NoEncryption"}
                                                                 :Prefix "string"
                                                                 :RoleARN "arn:aws:iam::909704556315:role/firehose_delivery_role"}})))
          (catch Exception e
            (is (nil? e) "Exception was thrown"))
          (finally
            (testing "delete streams and s3 bucket"
              (s3/delete-bucket cred stream-name)
              (s3/delete-bucket cred new-bucket-name)
              (delete-delivery-streams-matching-prefix))))))))
