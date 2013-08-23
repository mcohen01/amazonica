(ns amazonica.aws.sqs-async
  (:use [amazonica.core :only (kw->str)]
        [robert.hooke :only (add-hook)])
  (:import com.amazonaws.services.sqs.AmazonSQSAsyncClient))

(amazonica.core/set-client AmazonSQSAsyncClient *ns*)

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

(defn- delete-on-success [cred queue-url on-success]
  (fn [request result]
    (on-success request result)
    (doseq [id (message-ids result)]
      (delete-message-async
        cred
        :queue-url queue-url
        :receipt-handle id))))

(defn- delete-on-receive
  [f cred & args]
  (let [f (partial f cred)
        attrs (apply hash-map args)]
    (if (:delete attrs)
      (let
        [queue-url (:queue-url attrs)
         on-success (:on-success attrs)
         new-on-success (delete-on-success cred queue-url on-success)
         attrs (assoc attrs :on-success new-on-success)]
        (apply f (flatten (seq attrs))))
      (apply f args))))

(add-hook #'create-queue-async #'attr-keys->str)

(add-hook #'receive-message-async #'delete-on-receive)
