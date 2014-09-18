(ns amazonica.aws.dynamodb
  "Amazon DyanmoDB support."
  (:use [amazonica.core :only (accessors coerce-value
                               register-coercions set-fields IMarshall)]
        [clojure.algo.generic.functor :only (fmap)])
  (:import [com.amazonaws.services.dynamodb
              AmazonDynamoDBAsyncClient
              AmazonDynamoDBClient]
           [com.amazonaws.services.dynamodb.model
             AttributeValue
             Key
             ProvisionedThroughput]))


(extend-protocol IMarshall
  AttributeValue
  (marshall [obj]
    (amazonica.core/marshall
      (some
        #(.invoke % obj (make-array Object 0))
        (accessors (class obj) true)))))

(register-coercions
  AttributeValue
  (fn [value]
    (let [attr (AttributeValue.)]
      (if (coll? value)
        (if (map? value)
          (set-fields attr value)
          (if (integer? (first value))
            (.setNS attr (fmap str value))
            (.setSS attr (fmap str value))))
        (if (integer? value)
          (.setN attr (str value))
          (.setS attr value)))
      attr))
  Key
  (fn [value]
    (let [key (Key.)]
      (if (map? value)
        (set-fields key value)
        (if (sequential? value)
          (do
            (.setHashKeyElement key
              (coerce-value (first value) AttributeValue))
            (.setRangeKeyElement key
              (coerce-value (last value) AttributeValue)))
          (.setHashKeyElement key
            (coerce-value value AttributeValue))))
      key))
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