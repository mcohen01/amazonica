(ns amazonica.aws.kinesisfirehose
  (:require [amazonica.core :as amz]
            [clojure.string :as string])
  (:import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient
           java.nio.ByteBuffer))

(set! *warn-on-reflection* true)

(amz/set-client AmazonKinesisFirehoseClient *ns*)

(defn- join-as-csv [list-of-elements]
  (->> list-of-elements
       (map str)
       (map #(str "\"" (.replaceAll ^String % "\"" "\"\"") "\""))
       (string/join ","))) 

(defn ->bytes [data]
  (cond (instance? ByteBuffer data)             data
        (instance? (class (byte-array 0)) data) (ByteBuffer/wrap data)
        (string? data)                          (ByteBuffer/wrap (.getBytes ^String (str data "\n")))
        (sequential? data)                      (ByteBuffer/wrap (.getBytes ^String (str (join-as-csv data) "\n")))
        (map? data)                             (ByteBuffer/wrap (.getBytes ^String (pr-str data)))
        :else                                   nil))

(defonce ^:dynamic *list-delivery-streams-default-limit* 10000)

(defn- take-until
  "Like take-while, but includes the result that made the predicate fail."
  [pred coll]
  (lazy-seq
   (when-let [s (seq coll)]
     (let [f (first s)]
       (if (pred f)
         (cons f (take-until pred (rest s)))
         (list f))))))

(defn maybe-update-in
  "Like update-in, but returns the original map if the key doesn't exist, or the update func returns nil."
  [m path func & args]
  (if-let [e (get-in m path)]
    (let [r (apply func e args)]
      (if-not (nil? r)
        (assoc-in m path r)
        m))
    m))

(alter-var-root
 #'amazonica.aws.kinesisfirehose/list-delivery-streams
 (fn [f]
   (fn [& args]
     (let [{:keys [cred] [limit exclusive-start-delivery-stream-name :as args] :args} (amz/parse-args (first args) (rest args))
           call-list-delivery-streams (fn call-list-delivery-streams [cred limit exclusive-start-delivery-stream-name]
                                        (if exclusive-start-delivery-stream-name
                                          (f cred :limit limit :exclusive-start-delivery-stream-name exclusive-start-delivery-stream-name)
                                          (f cred :limit limit)))]
       (cond (number? limit)       (call-list-delivery-streams cred limit exclusive-start-delivery-stream-name)
             (map? limit)          (f cred limit)
             (and (keyword? limit) (even? (count args))) (apply f cred args)
             (nil? limit)          (let [rs (->> nil
                                                 (iterate #(call-list-delivery-streams cred *list-delivery-streams-default-limit* (last (:delivery-stream-names %))))
                                                 (drop 1)
                                                 (take-until :has-more-delivery-streams))]
                                     (assoc (last rs) :delivery-stream-names (mapcat :delivery-stream-names rs)))
             :else                 (throw (IllegalArgumentException. ^String (apply str "cannot call list-delivery-streams with : " args))))))))

(alter-var-root
 #'amazonica.aws.kinesisfirehose/put-record
 (fn [f]
   (fn put-record-impl [& args]
     (let [{:keys [cred] [delivery-stream-name data :as args] :args} (amz/parse-args (first args) (rest args))
           b (->bytes data)]
       (cond (and (string? delivery-stream-name) b)
             (if cred
               (f cred :delivery-stream-name delivery-stream-name :record {:data b})
               (f :delivery-stream-name delivery-stream-name :record {:data b}))
             
             (map? delivery-stream-name)
             (if cred
               (f cred (maybe-update-in delivery-stream-name [:record :data] ->bytes))
               (f (maybe-update-in delivery-stream-name [:record :data] ->bytes)))
             
             (and (keyword? delivery-stream-name) (even? (count args)))
             (put-record-impl cred (apply array-map args))
             
             :else
             (throw (IllegalArgumentException. ^String (apply str "cannot call put-record with : " args))))))))

(alter-var-root
 #'amazonica.aws.kinesisfirehose/put-record-batch
 (fn [f]
   (fn put-record-batch-impl [& args]
     (let [{:keys [cred] [delivery-stream-name batch-data :as args] :args} (amz/parse-args (first args) (rest args))
           b (when (sequential? batch-data) (map ->bytes batch-data))]
       (cond (and (string? delivery-stream-name) (every? (complement nil?) b))
             (if cred
               (f cred :delivery-stream-name delivery-stream-name :records (vec (map #(do {:data %}) b)))
               (f :delivery-stream-name delivery-stream-name :records (vec (map #(do {:data %}) b))))
             
             (map? delivery-stream-name)
             (if cred
               (f cred (assoc delivery-stream-name :records (map #(maybe-update-in % [:data] ->bytes) (:records delivery-stream-name))))
               (f (assoc delivery-stream-name :records (map #(maybe-update-in % [:data] ->bytes) (:records delivery-stream-name)))))
             
             (and (keyword? delivery-stream-name) (even? (count args)))
             (put-record-batch-impl cred (apply array-map args))
             
             :else
             (throw (IllegalArgumentException. ^String (apply str "cannot call put-record-batch with : " args))))))))

(alter-meta! #'amazonica.aws.kinesisfirehose/list-delivery-streams assoc :arglists '([] [creds] [creds limit] [creds limit exclusive-start-delivery-stream-name]) :doc "Lists available firehose streams.")
(alter-meta! #'amazonica.aws.kinesisfirehose/put-record-batch assoc :arglists '([delivery-stream-name batch-data] [creds delivery-stream-name batch-data]) :doc "Puts a batch of records onto a firehose stream.")
(alter-meta! #'amazonica.aws.kinesisfirehose/put-record assoc :arglists '([delivery-stream-name data] [creds delivery-stream-name data]) :doc "Puts a record onto a firehose stream.")
