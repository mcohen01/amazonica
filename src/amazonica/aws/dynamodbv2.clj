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
             ProvisionedThroughput]))


(extend-protocol IMarshall
  AttributeValue
  (marshall [obj]
    (marshall
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