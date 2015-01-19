(ns amazonica.aws.s3transfer
  (:use [amazonica.core :only (IMarshall marshall coerce-value stack->string)])
  (:import [com.amazonaws.event
            ProgressEvent
            ProgressListener]
           [com.amazonaws.services.s3.transfer            
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

(defn transfer
  [obj]
  {:add-progress-listener    (add-listener obj)
   :get-description          #(.getDescription obj)
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
  
  Download
  (marshall [obj]
    (let [t (transfer obj)]
      ((:add-progress-listener t) (partial default-listener t))
      (merge (transfer obj)
             {:key             #(.getKey obj)
              :bucket-name     #(.getBucketName obj)
              :object-metadata #(marshall (.getObjectMetadata obj))})))
  
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
     :event (condp = (.getEventCode obj)
              ProgressEvent/STARTED_EVENT_CODE        :started
              ProgressEvent/COMPLETED_EVENT_CODE      :completed
              ProgressEvent/FAILED_EVENT_CODE         :failed
              ProgressEvent/CANCELED_EVENT_CODE       :cancelled
              ProgressEvent/RESET_EVENT_CODE          :reset
              ProgressEvent/PREPARING_EVENT_CODE      :preparing
              ProgressEvent/PART_STARTED_EVENT_CODE   :part-started
              ProgressEvent/PART_COMPLETED_EVENT_CODE :part-completed
              ProgressEvent/PART_FAILED_EVENT_CODE    :part-failed
              nil)}))

(amazonica.core/set-client TransferManager *ns*)
