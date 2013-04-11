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

(defn attr-keys->str
  [f cred & args]
  (let [m    (apply hash-map args)
        mm   (merge-with               
               (fn [_ e] e)
               m
               (keys->str (:attributes m)))
        rval (interleave (keys mm) (vals mm))]
    (apply (partial f cred) rval)))

(add-hook #'create-queue #'attr-keys->str)