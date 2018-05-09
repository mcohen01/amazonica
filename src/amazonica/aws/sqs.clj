(ns amazonica.aws.sqs
  (:use [amazonica.core :only (kw->str parse-args)]
        [clojure.walk]
        [robert.hooke :only (add-hook)])
  (:import com.amazonaws.services.sqs.AmazonSQSClient))

(amazonica.core/set-client AmazonSQSClient *ns*)

(defn- attr-keys->str
  [f cred & args]
  (let [arg-map (parse-args cred args)
        func    (if (contains? arg-map :cred)
                    (partial f cred)
                    f)
        attrs   (if (even? (count (:args arg-map)))
                    (mapcat identity
                            (-> (apply hash-map (:args arg-map))
                                (update-in [:attributes] stringify-keys)))
                    (:args arg-map))]
    (apply func attrs)))

(defn- message-ids
  [messages]
  (reduce
    #(conj % (:receipt-handle %2))
    []
    (:messages messages)))

(defn- delete-on-receive
  [f cred & args]
  (let [arg-map (parse-args cred args)
        attrs   (if (even? (count (:args arg-map)))
                    (apply hash-map (:args arg-map))
                    (:args arg-map))
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

(alter-var-root
  #'amazonica.aws.sqs/get-queue-attributes
  (fn [f]
    (fn [& args]
      (let [a (if (or (= 1 (count args))
                      (nil? (last args))
                      (empty? (last args)))
                  (seq (vector (first args) ["All"]))
                  args)]
      (:attributes (apply f a))))))

(add-hook #'create-queue #'attr-keys->str)

(add-hook #'receive-message #'delete-on-receive)

(defn find-queue [& s]
  (some
    (fn [^String q]
      (when (.contains q (or (second s) (first s))) q))
    (:queue-urls (list-queues (first s)))))

(defn arn [q] (-> q get-queue-attributes :QueueArn))

(defn assign-dead-letter-queue
  [queue dlq max-receive-count]
  (set-queue-attributes
    queue {"RedrivePolicy"
           (str "{\"maxReceiveCount\": " max-receive-count ",
                  \"deadLetterTargetArn\": \"" (arn dlq) "\"}")}))
