(ns amazonica.aws.dynamodbv2
  "Amazon DyanmoDBV2 support - Local Secondary Indexes"
  (:use [amazonica.core :only (accessors coerce-value marshall
                               register-coercions set-fields IMarshall)]
        [clojure.algo.generic.functor :only (fmap)])
  (:import [com.amazonaws.services.dynamodbv2
              AmazonDynamoDBAsyncClient
              AmazonDynamoDBClient]
           [com.amazonaws.services.dynamodbv2.model
             AttributeValue
             ProvisionedThroughput]
           java.nio.ByteBuffer))

(defn- parse-number
  [^String ns]
  (if (.contains ns ".")
    (Double/parseDouble ns)
    (Long/parseLong ns)))

(extend-protocol IMarshall
  AttributeValue
  (marshall [obj]
    (let [[type val] (some #(when (val %) %) (dissoc (bean obj) :class))]
      (marshall (case type
                  (:s :b :BOOL) val
                  (:SS :BS) (into #{} val)
                  :n (parse-number val)
                  :NS (into #{} (map parse-number val))
                  :l (into [] (map marshall val))
                  :m (into {} (map (fn [[k v]] [k (marshall v)]) val))
                  :NULL nil)))))

(def ^:private byte-array-type (class (byte-array 0)))

(defn- name-of-first-key [value]
  (name (key (first value))))

(defn- scalar-attribute-value? [value]
  (and (map? value)
       (= 1 (count value))
       (= 1 (count (name-of-first-key value)))))

(register-coercions
  AttributeValue
  (fn [value]
      (let [attr (AttributeValue.)]
        (if (coll? value)
          (cond
           (scalar-attribute-value? value) (set-fields attr value)
           (map? value) (.setM attr (into {} (map (fn [[k v]] [(name k) (coerce-value v AttributeValue)]) value)))
           (vector? value) (.setL attr (map (fn [v] (coerce-value v AttributeValue)) value))
           :else
           (let [fvalue (first value)]
             (when-not (every? #(instance? (class fvalue) %) value)
               (throw (ex-info "Cannot provide heterogenous collection as value"
                               {:value value})))
             (cond
              (number? fvalue) (.setNS attr (fmap str value))
              (string? fvalue) (.setSS attr value)
              (instance? ByteBuffer fvalue) (.setBS attr value)

              (instance? byte-array-type fvalue)
              (.setBS attr (fmap #(ByteBuffer/wrap %) value))
              :else
              (throw (ex-info
                      (format "Values of type %s cannot be used with dynamodb"
                              (class fvalue))
                      {:value value})))))
          (cond
           (nil? value) (.setNULL attr true)
           (number? value) (.setN attr (str value))
           (string? value) (.setS attr value)
           (instance? Boolean value) (.setBOOL attr value)
           (instance? byte-array-type value) (.setB attr (ByteBuffer/wrap value))
           (instance? ByteBuffer value) (.setB attr value)
           :else (throw (ex-info (format "Value of type %s cannot be used with dynamodb"
                                         (class value))
                                 {:value value}))))
        attr))
  ProvisionedThroughput
  (fn [value]
    (let [pt (ProvisionedThroughput.)]
      (if (map? value)
        (set-fields pt value)
        (if (sequential? value)
          (do
            (.setReadCapacityUnits pt (first value))
            (.setWriteCapacityUnits pt (last value)))))
      pt)))

(amazonica.core/set-client AmazonDynamoDBClient *ns*)
