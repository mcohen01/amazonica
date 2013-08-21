(ns amazonica.aws.s3transfer
  (:use [amazonica.core :only (IMarshall marshall register-coercions coerce-value)]
        [clojure.algo.generic.functor :only (fmap)])
  (:import [com.amazonaws.services.s3.transfer
            TransferManager
            Transfer
            TransferProgress
            Upload])
  (:import [com.amazonaws.services.s3.model
            ProgressListener]))

(extend-protocol IMarshall
  TransferProgress
  (marshall [obj]
    {:total-bytes-to-transfer (.getTotalBytesToTransfer obj)
    :bytes-transferred (.getBytesTransferred obj)
    :percent-transferred (.getPercentTransferred obj)})

  Upload
  (marshall [obj]
    {:upload-result #(marshall (.waitForUploadResult obj))
     :add-progress-listener #(.addProgressListener obj %)
     :get-description #(.getDescription obj)
     :get-progress #(marshall (.getProgress obj))
     :get-state #(str (.getState obj))
     :is-done #(.isDone obj)
     :remove-progress-listener #(.removeProgressListener obj %)
     :wait-for-completion #(.waitForCompletion obj)
     :wait-for-exception #(marshall (.waitForException obj))}))

(register-coercions
  ProgressListener
  (fn [col]
    (proxy
      [ProgressListener] []
      (progressChanged [event]
        ((:progress-changed col) event)))))

(amazonica.core/set-client TransferManager *ns*)

(defn new-progress-listener
  "This helper function returns an object implementing the ProgressListener interface.
  The argument progress-changed is a function taking a single ProgressEvent argument."
  [progress-changed]
  (let [col {:progress-changed progress-changed}]
    (coerce-value col ProgressListener)))
