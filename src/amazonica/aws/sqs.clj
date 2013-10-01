(ns amazonica.aws.sqs
  (:use [amazonica.core :only (get-credentials kw->str)]
        [clojure.walk]
        [robert.hooke :only (add-hook)])
  (:import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
           com.amazonaws.services.sqs.AmazonSQSClient))

(amazonica.core/set-client AmazonSQSClient *ns*)

(defn- parse-args
  "Legacy support means credentials may or may not be passed
   as the first argument."
  [cred args]
  (if (instance? DefaultAWSCredentialsProviderChain (get-credentials cred))
      {:args (conj args cred)}
      {:args args :cred cred}))

(defn- attr-keys->str
  [f cred & args]
  (let [arg-map (parse-args cred args)
        func    (if (contains? arg-map :cred)
                    (partial f cred)
                    f)
        attrs   (apply hash-map (:args arg-map))]
    (apply func
           (mapcat identity
                   (update-in attrs [:attributes] stringify-keys)))))

(defn- message-ids
  [messages]
  (reduce
    #(conj % (:receipt-handle %2))
    []
    (:messages messages)))

(defn- delete-on-receive
  [f cred & args]
  (let [arg-map (parse-args cred args)
        attrs   (apply hash-map (:args arg-map))
        func    (if (contains? arg-map :cred)
                    {:rec-fn (partial f cred)
                     :del-fn (partial delete-message cred)}
                    {:rec-fn f
                     :del-fn delete-message})
        rval    (apply (:rec-fn func) (:args arg-map))]
    (if (:delete attrs)
      (doseq [id (message-ids rval)]
        (apply (:del-fn func)
               (mapcat identity
                       (assoc attrs :receipt-handle id)))))
    rval))

(add-hook #'create-queue #'attr-keys->str)

(add-hook #'receive-message #'delete-on-receive)