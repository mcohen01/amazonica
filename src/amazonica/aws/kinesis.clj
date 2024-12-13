(ns amazonica.aws.kinesis
  (:require [amazonica.core :as amz]
            [taoensso.nippy :as nippy]
            [clojure.algo.generic.functor :as functor])
  (:import [com.amazonaws.auth
            AWSCredentialsProvider
            AWSCredentials
            AWSCredentialsProviderChain
            DefaultAWSCredentialsProviderChain]
           com.amazonaws.internal.StaticCredentialsProvider
           com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
           [com.amazonaws.regions
            Region
            Regions]
           [com.amazonaws.services.kinesis
            AmazonKinesisClient]
           [com.amazonaws.services.kinesis.model
            Record]
           [com.amazonaws.services.kinesis.clientlibrary.interfaces
            IRecordProcessorCheckpointer]
           [com.amazonaws.services.kinesis.clientlibrary.interfaces.v2
            IRecordProcessorFactory
            IRecordProcessor
            IShutdownNotificationAware]
           [com.amazonaws.services.kinesis.clientlibrary.exceptions
            InvalidStateException
            KinesisClientLibDependencyException
            ShutdownException
            ThrottlingException]
           [com.amazonaws.services.kinesis.clientlibrary.lib.worker
            InitialPositionInStream
            KinesisClientLibConfiguration
            Worker
            Worker$Builder
            ShutdownReason]
           [com.amazonaws.services.kinesis.metrics.interfaces
            MetricsLevel]
           java.nio.ByteBuffer
           java.util.UUID))

(set! *warn-on-reflection* true)

(amz/set-client AmazonKinesisClient *ns*)

(defn- ->bytes
  [data]
  (if (instance? ByteBuffer data)
    data
    (ByteBuffer/wrap (nippy/freeze data))))

(alter-var-root
 #'amazonica.aws.kinesis/put-record
 (fn [f]
   (fn [& args]
     (let [parsed (amz/parse-args (first args) (rest args))
           args   (:args parsed)
           [stream data key & [seq-id]] args
           bytes  (->bytes data)
           putrec (->> (list (:cred parsed) stream bytes key)
                       (filter (complement nil?))
                       (apply partial f))]
       (if seq-id
         (putrec seq-id)
         (putrec))))))

(alter-var-root
 #'amazonica.aws.kinesis/put-records
 (fn [f]
   (fn [& args]
     (let [parsed (amz/parse-args (first args) (rest args))
           [stream data] (:args parsed)
           data-byte (map (fn [x] (update-in x [:data] ->bytes)) data)]
       (if (nil? (:cred parsed))
         (f :stream-name stream :records data-byte)
         (f (:cred parsed) :stream-name stream :records data-byte))))))

(defn unwrap
  "Get the contents of the given buffer as a byte-array, decoding as
  Nippy bytes if they appear to be Nippy encoded. If the ByteBuffer
  does not appear to contain Nippy data, the bytes found will be
  returned unchanged. This technique is inspired by ptaoussanis/faraday."
  [^java.nio.ByteBuffer byte-buffer]
  (let [byte-array (.array byte-buffer)
        serialized? (#'nippy/try-parse-header byte-array)]
    (if-not serialized?
      byte-array ; No Nippy header => assume non-nippy binary data
      (try ; Header match _may_ have been a fluke (though v. unlikely)
        (nippy/thaw byte-array)
        (catch Exception e
          byte-array)))))

(alter-var-root
 #'amazonica.aws.kinesis/get-shard-iterator
 (fn [f]
   (fn [& args]
     (:shard-iterator (apply f args)))))

(alter-var-root
 #'amazonica.aws.kinesis/get-records
 (fn [f]
   (fn [& args]
     (let [parsed      (amz/parse-args (first args) (rest args))
           args         (if (= 1 (count (:args parsed)))
                          (first (:args parsed))
                          (apply hash-map (seq (:args parsed))))
           deserializer (or (:deserializer args) unwrap)
           result (->>  (list (:cred parsed) args)
                        (filter (complement nil?))
                        (apply f))]
       (assoc result
              :records
              (functor/fmap
               (fn [record]
                 (update-in record [:data] (fn [d] (deserializer d))))
               (:records result)))))))

(defn marshall
  [deserializer ^Record record]
  {:approximate-arrival-timestamp (amz/marshall (.getApproximateArrivalTimestamp record))
   :encryption-type               (.getEncryptionType record)
   :sequence-number               (.getSequenceNumber record)
   :partition-key                 (.getPartitionKey record)
   :data                          (deserializer (.getData record))})

(defn- mark-checkpoint [^IRecordProcessorCheckpointer checkpointer]
  (try
    (.checkpoint checkpointer)
    true
    (catch ShutdownException se true)
    (catch InvalidStateException ise false)
    (catch KinesisClientLibDependencyException de false)
    (catch ThrottlingException te
      (println "sleeping for 3s due to throttling....")
      (Thread/sleep 3000)
      false)))

(def ^:dynamic *shard-id* nil)

(defn- processor-factory
  [processor deserializer checkpoint]
  (reify IRecordProcessorFactory
    (createProcessor [_this]
      (let [next-check (atom 0)
            shard-id (atom nil)]
        (reify IRecordProcessor
          (initialize [_this initialisation-input]
            (reset! shard-id (.getShardId initialisation-input)))
          (shutdown [_this shutdown-input]
            (let [reason (.getShutdownReason shutdown-input)
                  checkpointer (.getCheckpointer shutdown-input)]
              (when (or (= ShutdownReason/TERMINATE reason)
                        (= "TERMINATE" reason))
                (some (fn [_] (mark-checkpoint checkpointer)) (repeat 5 nil)))))
          (processRecords [_this process-records-input]
            (binding [*shard-id* @shard-id]
              (let [records (vec (seq (.getRecords process-records-input)))
                    checkpointer (.getCheckpointer process-records-input)]
                (when (or (processor (functor/fmap (partial marshall deserializer)
                                                   records))
                          (and checkpoint
                               (> (System/currentTimeMillis) @next-check)))
                  (when checkpoint
                    (reset! next-check
                            (+' (System/currentTimeMillis)
                                (*' 1000 checkpoint))))
                  (some (fn [_] (mark-checkpoint checkpointer)) (repeat 5 nil))))))
          IShutdownNotificationAware
          (shutdownRequested [_this checkpointer]
            (some (fn [_] (mark-checkpoint checkpointer)) (repeat 5 nil))))))))

(defn- kinesis-client-lib-configuration
  "Instantiate a KinesisClientLibConfiguration instance."
  ^KinesisClientLibConfiguration [^AWSCredentialsProvider provider
                                  {:keys [app
                                          dynamodb-credentials-provider
                                          cloudwatch-credentials-provider
                                          stream
                                          worker-id
                                          endpoint
                                          dynamodb-endpoint
                                          billing-mode
                                          initial-position-in-stream
                                          ^java.util.Date initial-position-in-stream-date
                                          failover-time-millis
                                          shard-sync-interval-millis
                                          max-records
                                          idle-time-between-reads-in-millis
                                          call-process-records-even-for-empty-record-list
                                          parent-shard-poll-interval-millis
                                          cleanup-leases-upon-shard-completion
                                          common-client-config
                                          kinesis-client-config
                                          dynamodb-client-config
                                          cloud-watch-client-config
                                          user-agent
                                          task-backoff-time-millis
                                          metrics-level
                                          metrics-buffer-time-millis
                                          metrics-max-queue-size
                                          validate-sequence-number-before-checkpointing
                                          region-name
                                          initial-lease-table-read-capacity
                                          initial-lease-table-write-capacity]
                                   :or {worker-id (str (UUID/randomUUID))}}]
  (cond-> (KinesisClientLibConfiguration. (name app)
                                          (name stream)
                                          provider
                                          (or dynamodb-credentials-provider provider)
                                          (or cloudwatch-credentials-provider provider)
                                          (name worker-id))
    endpoint
    (.withKinesisEndpoint endpoint)

    dynamodb-endpoint
    (.withDynamoDBEndpoint dynamodb-endpoint)

    billing-mode
    (.withBillingMode billing-mode)

    initial-position-in-stream
    (.withInitialPositionInStream
     (InitialPositionInStream/valueOf (name initial-position-in-stream)))

    initial-position-in-stream-date
    (.withTimestampAtInitialPositionInStream
     initial-position-in-stream-date)

    failover-time-millis
    (.withFailoverTimeMillis failover-time-millis)

    shard-sync-interval-millis
    (.withShardSyncIntervalMillis shard-sync-interval-millis)

    max-records
    (.withMaxRecords max-records)

    idle-time-between-reads-in-millis
    (.withIdleTimeBetweenReadsInMillis idle-time-between-reads-in-millis)

    call-process-records-even-for-empty-record-list
    (.withCallProcessRecordsEvenForEmptyRecordList
     call-process-records-even-for-empty-record-list)

    parent-shard-poll-interval-millis
    (.withParentShardPollIntervalMillis
     parent-shard-poll-interval-millis)

    cleanup-leases-upon-shard-completion
    (.withCleanupLeasesUponShardCompletion
     cleanup-leases-upon-shard-completion)

    common-client-config
    (.withCommonClientConfig common-client-config)

    kinesis-client-config
    (.withKinesisClientConfig kinesis-client-config)

    dynamodb-client-config
    (.withDynamoDBClientConfig dynamodb-client-config)

    cloud-watch-client-config
    (.withCloudWatchClientConfig cloud-watch-client-config)

    user-agent
    (.withUserAgent user-agent)

    task-backoff-time-millis
    (.withTaskBackoffTimeMillis task-backoff-time-millis)

    metrics-level
    (.withMetricsLevel (MetricsLevel/valueOf (name metrics-level)))

    metrics-buffer-time-millis
    (.withMetricsBufferTimeMillis metrics-buffer-time-millis)

    metrics-max-queue-size
    (.withMetricsMaxQueueSize metrics-max-queue-size)

    validate-sequence-number-before-checkpointing
    (.withValidateSequenceNumberBeforeCheckpointing validate-sequence-number-before-checkpointing)

    region-name
    (.withRegionName region-name)

    initial-lease-table-read-capacity
    (.withInitialLeaseTableReadCapacity initial-lease-table-read-capacity)

    initial-lease-table-write-capacity
    (.withInitialLeaseTableWriteCapacity initial-lease-table-write-capacity)))

(defn worker
  "Instantiate a kinesis Worker."
  [& args]
  (let [opts (if (associative? (first args))
               (first args)
               (apply hash-map args))
        {:keys [processor
                deserializer
                checkpoint
                credentials
                dynamodb-adaptor-client?
                ^String region-name
                ^String endpoint]
         :or   {checkpoint 60
                deserializer unwrap
                endpoint "kinesis.us-east-1.amazonaws.com"}} opts
        factory           (processor-factory processor deserializer checkpoint)
        creds             (amz/get-credentials credentials)
        provider          (if (instance? AWSCredentials creds)
                            (StaticCredentialsProvider. creds)
                            creds)
        config            (kinesis-client-lib-configuration provider (assoc opts :endpoint endpoint))
        worker-identifier (.getWorkerIdentifier config)]
    [(-> (Worker$Builder.)
         (.recordProcessorFactory ^IRecordProcessorFactory factory)
         (.config ^KinesisClientLibConfiguration config)
         (cond->
          dynamodb-adaptor-client?
           ;; this will result in some warnings at debug from the kinesis client lib as it will try to set the region/endpoint on this client.
           ;; These are safe to ignore as we pre-configure the correct values
           (.kinesisClient
            (doto (if provider
                    (AmazonDynamoDBStreamsAdapterClient. ^AWSCredentialsProvider provider (.getKinesisClientConfiguration config))
                    (AmazonDynamoDBStreamsAdapterClient. (.getKinesisClientConfiguration config)))
              (cond->
               region-name ^AmazonDynamoDBStreamsAdapterClient (.setRegion (Region/getRegion (Regions/fromName region-name))))
              (cond->
               endpoint ^AmazonDynamoDBStreamsAdapterClient (.setEndpoint endpoint)))))
         (.build)) worker-identifier]))

(defn worker!
  "Instantiate a new kinesis Worker and invoke its run method in a
   separate thread. Return the identifier of the Worker."
  [& args]
  (let [[^Worker worker uuid] (apply worker args)]
    (future (.run worker))
    uuid))
