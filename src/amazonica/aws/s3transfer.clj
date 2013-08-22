(ns amazonica.aws.s3transfer
  (:use [amazonica.core :only (IMarshall marshall register-coercions coerce-value)]
        [clojure.algo.generic.functor :only (fmap)])
  (:import [com.amazonaws.services.s3.model             
            ProgressEvent
            ProgressListener]
           [com.amazonaws.services.s3.transfer            
            Download
            TransferManager
            Transfer
            TransferProgress
            Upload]))

(defn transfer
  [obj]
  {:add-progress-listener    #(.addProgressListener obj %)
   :get-description          #(.getDescription obj)
   :get-progress             #(marshall (.getProgress obj))
   :get-state                #(str (.getState obj))
   :is-done                  #(.isDone obj)
   :remove-progress-listener #(.removeProgressListener obj %)
   :wait-for-completion      #(.waitForCompletion obj)
   :wait-for-exception       #(marshall (.waitForException obj))})


(extend-protocol IMarshall
  TransferProgress
  (marshall [obj]
    {:total-bytes-to-transfer (.getTotalBytesToTransfer obj)
     :bytes-transferred       (.getBytesTransferred obj)
     :percent-transferred     (.getPercentTransferred obj)})

  Upload
  (marshall [obj]
    (merge (transfer obj)
           {:upload-result #(marshall (.waitForUploadResult obj))}))
  
  Download
  (marshall [obj]
    (merge (transfer obj)
           {:bucket-name     #(.getBucketName obj)
            :abort           #(.abort obj)
            :key             #(.getKey obj)
            :object-metadata #(marshall (.getObjectMetadata obj))}))
  
  ProgressEvent
  (marshall [obj]
    {:bytes-transferred (.getBytesTransferred obj)
     :event (condp = (.getEventCode obj)
              ProgressEvent/STARTED_EVENT_CODE        :started
              ProgressEvent/COMPLETED_EVENT_CODE      :completed
              ProgressEvent/FAILED_EVENT_CODE         :failed
              ProgressEvent/CANCELED_EVENT_CODE       :cancelled
              ProgressEvent/PART_STARTED_EVENT_CODE   :part-started
              ProgressEvent/PART_COMPLETED_EVENT_CODE :part-completed
              ProgressEvent/PART_FAILED_EVENT_CODE    :part-failed
              nil)}))

(register-coercions
  ProgressListener
  (fn [col]
    (reify ProgressListener
      (progressChanged [this event]
        ((:progress-changed col) (marshall event))))))

(amazonica.core/set-client TransferManager *ns*)

(defn new-progress-listener
  "This helper function returns an object implementing the ProgressListener interface.
  The argument progress-changed is a function taking a single ProgressEvent argument."
  [progress-changed]
  (let [col {:progress-changed progress-changed}]
    (coerce-value col ProgressListener)))
