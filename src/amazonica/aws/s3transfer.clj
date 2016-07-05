(ns amazonica.aws.s3transfer
  (:require [amazonica.core :refer [IMarshall marshall coerce-value stack->string]]
            [amazonica.aws.s3])
  (:import [com.amazonaws.event
            ProgressEvent
            ProgressEventType
            ProgressListener]
           [com.amazonaws.services.s3.transfer            
              Copy
              Download
              MultipleFileUpload
              MultipleFileDownload
              Upload
              TransferManager
              Transfer
              TransferProgress]))

(defn- default-listener [transfer e]
  (cond (= (:event e) :failed)    (println ((:wait-for-exception transfer)))
        (= (:event e) :completed) (println "Transfer complete.")))
      
(defn add-listener
  [obj]
  (fn [f]
    (let [listener (reify ProgressListener
                     (progressChanged [this event]
                       (f (marshall event))))]
      (.addProgressListener obj listener)
      listener)))

(defmacro wait
  [transfer & body]
  `(fn []
     (.waitForCompletion ~transfer)
     (do ~@body)))

(defn transfer
  [obj]
  {:transfer                 obj
   :add-progress-listener    (add-listener obj)
   :get-description          (wait obj (.getDescription obj))
   :get-progress             #(marshall (.getProgress obj))
   :get-state                #(str (.getState obj))
   :is-done                  #(.isDone obj)
   :abort                    #(.abort obj)
   :pause                    #(.pause obj)
   :remove-progress-listener #(.removeProgressListener obj %)
   :wait-for-completion      #(.waitForCompletion obj)
   :wait-for-exception       #(stack->string (.waitForException obj))})


(extend-protocol IMarshall
  TransferProgress
  (marshall [obj]
    {:total-bytes-to-transfer (.getTotalBytesToTransfer obj)
     :bytes-transferred       (.getBytesTransferred obj)
     :percent-transferred     (.getPercentTransferred obj)})

  Upload
  (marshall [obj]
    (let [t (transfer obj)]
      ((:add-progress-listener t) (partial default-listener t))
      (merge (transfer obj)
             {:try-pause     #(.tryPause obj %)
              :upload-result #(marshall (.waitForUploadResult obj))})))
  
  Copy
  (marshall [obj]
    (let [t (transfer obj)]
      ((:add-progress-listener t) (partial default-listener t))
      (merge t {:copy-result #(marshall (.waitForCopyResult obj))})))

  Download
  (marshall [obj]
    (let [t (transfer obj)]
      ((:add-progress-listener t) (partial default-listener t))
      (merge t {:key             (wait obj (.getKey obj))
                :bucket-name     (wait obj (.getBucketName obj))
                :object-metadata (wait obj (marshall (.getObjectMetadata obj)))})))
  
  MultipleFileUpload
  (marshall [obj]
    (merge (transfer obj)
           {:bucket-name #(.getBucketName obj)
            :key-prefix  #(.getKeyPrefix obj)}))
  
  MultipleFileDownload
  (marshall [obj]
    (merge (transfer obj)
           {:abort       #(.abort obj)
            :bucket-name #(.getBucketName obj)
            :key-prefix  #(.getKeyPrefix obj)}))

  ProgressEvent
  (marshall [obj]
    {:bytes-transferred (.getBytesTransferred obj)
     :event (condp = (.getEventType obj)
              ProgressEventType/TRANSFER_STARTED_EVENT        :started
              ProgressEventType/TRANSFER_COMPLETED_EVENT      :completed
              ProgressEventType/TRANSFER_FAILED_EVENT         :failed
              ProgressEventType/TRANSFER_CANCELED_EVENT       :cancelled
              ProgressEventType/TRANSFER_PREPARING_EVENT      :preparing
              ProgressEventType/REQUEST_BYTE_TRANSFER_EVENT   :transfered
              ProgressEventType/TRANSFER_PART_STARTED_EVENT   :part-started
              ProgressEventType/TRANSFER_PART_COMPLETED_EVENT :part-completed
              ProgressEventType/TRANSFER_PART_FAILED_EVENT    :part-failed
              nil)}))

(amazonica.core/set-client TransferManager *ns*)
