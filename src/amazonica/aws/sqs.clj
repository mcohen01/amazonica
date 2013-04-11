(ns amazonica.aws.sqs
  (:use [amazonica.core :only (kw->str)]
        [robert.hooke :only (add-hook)])
  (:import com.amazonaws.services.sqs.AmazonSQSClient))

(amazonica.core/set-client AmazonSQSClient *ns*)

(defn- keys->str
  [attrs]
  {:attributes
    (into {}
      (for [[k v] attrs]
        (hash-map (kw->str k) (str v))))})

(defn- attr-keys->str
  [f cred & args]
  (let [m    (apply hash-map args)
        mm   (merge-with               
               (fn [_ e] e)
               m
               (keys->str (:attributes m)))
        rval (interleave (keys mm) (vals mm))]
    (apply (partial f cred) rval)))

(defn- message-ids
  [messages]
  (reduce
    #(conj % (:receipt-handle %2))
    []
    (:messages messages)))

(defn- delete-on-receive
  [f cred & args]
  (let [attrs (apply hash-map args)
        rval  (apply (partial f cred) args)]
    (if (:delete attrs)
      (doseq [id (message-ids rval)]
        (delete-message 
          cred
          :queue-url (:queue-url attrs)
          :receipt-handle id)))
    rval))

(add-hook #'create-queue #'attr-keys->str)

(add-hook #'receive-message #'delete-on-receive)