(ns amazonica.aws.kinesis
  (:require [amazonica.core :as amz]
            [taoensso.nippy :as nippy]
            [clojure.algo.generic.functor :as functor])
  (:import [com.amazonaws.auth
              AWSCredentials
              AWSCredentialsProviderChain
              DefaultAWSCredentialsProviderChain]
           com.amazonaws.internal.StaticCredentialsProvider
           com.amazonaws.services.kinesis.AmazonKinesisClient
           [com.amazonaws.services.kinesis.clientlibrary.interfaces
              IRecordProcessor
              IRecordProcessorFactory]
           [com.amazonaws.services.kinesis.clientlibrary.exceptions
              InvalidStateException
              ShutdownException
              ThrottlingException]
           [com.amazonaws.services.kinesis.clientlibrary.lib.worker
              KinesisClientLibConfiguration
              Worker]
           java.nio.ByteBuffer
           java.util.UUID))

(amz/set-client AmazonKinesisClient *ns*)

(alter-var-root
  #'amazonica.aws.kinesis/put-record
  (fn [f]
    (fn [& args]
      (let [stream (first args)
            data   (second args)
            key    (nth args 2)
            bytes  (ByteBuffer/wrap (nippy/freeze data))
            putrec (partial f stream bytes key)]
      (if (= 3 (count args))
          (putrec)
          (putrec (nth args 3)))))))

(defn- unwrap
  [byte-buffer]
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
      (let [result (apply f args)]
        (assoc result
               :records
               (functor/fmap
                 (fn [record]
                   (update-in record [:data] (fn [d] (unwrap d))))
                   (:records result)))))))

(defn- marshall
  [record]
  {:sequence-number (.getSequenceNumber record)
   :partition-key   (.getPartitionKey record)
   :data            (unwrap (.getData record))})

(defn- mark-checkpoint [checkpointer _]
  (try
    (.checkpoint checkpointer)
    true
    (catch ShutdownException se
      true)
    (catch ThrottlingException te
      (println "sleeping for 3s due to throttling....")
      (Thread/sleep 3000)
      false)
    (catch InvalidStateException ise
      false)))

(defn- processor-factory
  [processor checkpoint next-check]
  (reify IRecordProcessorFactory
    (createProcessor [this]
      (reify IRecordProcessor
        (initialize [this shard-id])
        (shutdown [this checkpointer reason]
          (if (= "TERMINATE" reason)
              (some (partial mark-checkpoint checkpointer) [1 2 3 4 5])))
        (processRecords [this records checkpointer]
          (if (or (processor (functor/fmap marshall (vec (seq records))))
                  (> (System/currentTimeMillis) @next-check))
              (do (reset! next-check (+' (System/currentTimeMillis) checkpoint))
                  (some (partial mark-checkpoint checkpointer) [1 2 3 4 5]))))))))

(defn worker!
  [& args]
  (let [opts (if (associative? (first args))
                 (first args)
                 (apply hash-map args))
        {:keys [app stream processor checkpoint credentials]
         :or   {checkpoint   60000
                credentials {:endpoint "kinesis.us-east-1.amazonaws.com"}}} opts
        next-check (atom 0)
        factory  (processor-factory processor checkpoint next-check)
        uuid     (str (UUID/randomUUID))
        creds    (amz/get-credentials credentials)
        provider (if (instance? AWSCredentials creds)
                     (StaticCredentialsProvider. creds)
                     creds)
        config   (KinesisClientLibConfiguration. app
                                                 stream
                                                 ;(:endpoint credentials)
                                                 provider
                                                 uuid)]
    (future (.run (Worker. factory config)))
    uuid))