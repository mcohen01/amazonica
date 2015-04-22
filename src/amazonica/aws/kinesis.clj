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
           com.amazonaws.services.kinesis.AmazonKinesisClient
           [com.amazonaws.services.kinesis.model
            Record]
           [com.amazonaws.services.kinesis.clientlibrary.interfaces
              IRecordProcessor
              IRecordProcessorCheckpointer
              IRecordProcessorFactory]
           [com.amazonaws.services.kinesis.clientlibrary.exceptions
              InvalidStateException
              KinesisClientLibDependencyException
              ShutdownException
              ThrottlingException]
           [com.amazonaws.services.kinesis.clientlibrary.types
            ShutdownReason]
           [com.amazonaws.services.kinesis.clientlibrary.lib.worker
              InitialPositionInStream
              KinesisClientLibConfiguration
              Worker]
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
  [^java.nio.ByteBuffer byte-buffer]
  (let [b (byte-array (.remaining byte-buffer))]
    (.get byte-buffer b)
    (nippy/thaw b)))

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
  {:sequence-number (.getSequenceNumber record)
   :partition-key   (.getPartitionKey record)
   :data            (deserializer (.getData record))})

(defn- mark-checkpoint [^IRecordProcessorCheckpointer checkpointer _]
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

(defn- processor-factory
  [processor deserializer checkpoint next-check]
  (reify IRecordProcessorFactory
    (createProcessor [this]
      (reify IRecordProcessor
        (initialize [this shard-id])
        (shutdown [this checkpointer reason]
          (if (or (= ShutdownReason/TERMINATE reason)
                  (= "TERMINATE" reason))
              (some (partial mark-checkpoint checkpointer) [1 2 3 4 5])))
        (processRecords [this records checkpointer]
          (if (or (processor (functor/fmap (partial marshall deserializer)
                                           (vec (seq records))))
                  (and checkpoint
                       (> (System/currentTimeMillis) @next-check)))
              (do (if checkpoint
                      (reset! next-check
                              (+' (System/currentTimeMillis)
                                  (*' 1000 checkpoint))))
                  (some (partial mark-checkpoint checkpointer) [1 2 3 4 5]))))))))

(defn- kinesis-client-lib-configuration
  "Instantiate a KinesisClientLibConfiguration instance."
  ^KinesisClientLibConfiguration [^AWSCredentialsProvider provider
   {:keys [app
           stream
           worker-id
           endpoint
           initial-position-in-stream
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
           metrics-buffer-time-millis
           metrics-max-queue-size
           validate-sequence-number-before-checkpointing
           region-name]
    :or {worker-id (str (UUID/randomUUID))
         endpoint "kinesis.us-east-1.amazonaws.com"}}]
  (cond-> (KinesisClientLibConfiguration. (name app)
                                          (name stream)
                                          provider
                                          (name worker-id))

          endpoint
          (.withKinesisEndpoint endpoint)

          initial-position-in-stream
          (.withInitialPositionInStream
           (InitialPositionInStream/valueOf (name initial-position-in-stream)))

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

          metrics-buffer-time-millis
          (.withMetricsBufferTimeMillis metrics-buffer-time-millis)

          metrics-max-queue-size
          (.withMetricsMaxQueueSize metrics-max-queue-size)

          validate-sequence-number-before-checkpointing
          (.withValidateSequenceNumberBeforeCheckpointing validate-sequence-number-before-checkpointing)

          region-name
          (.withRegionName region-name)))

(defn worker
  "Instantiate a kinesis Worker."
  [& args]
  (let [opts (if (associative? (first args))
                 (first args)
                 (apply hash-map args))
        {:keys [processor deserializer checkpoint credentials]
         :or   {checkpoint 60
                deserializer unwrap}} opts
        next-check (atom 0)
        factory  (processor-factory processor deserializer checkpoint next-check)
        creds    (amz/get-credentials credentials)
        provider (if (instance? AWSCredentials creds)
                     (StaticCredentialsProvider. creds)
                     creds)
        config   (kinesis-client-lib-configuration provider opts)]
    [(Worker. factory config) (.getWorkerIdentifier config)]))

(defn worker!
  "Instantiate a new kinesis Worker and invoke its run method in a
   separate thread. Return the identifier of the Worker."
  [& args]
  (let [[^Worker worker uuid] (apply worker args)]
    (future (.run worker))
    uuid))
