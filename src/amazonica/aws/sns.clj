(ns amazonica.aws.sns
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.sns.AmazonSNSClient
           [com.amazonaws.services.sns.model MessageAttributeValue]))

(amz/register-coercions
  MessageAttributeValue
  (fn [value]
    (cond
      (string? value) (doto (MessageAttributeValue.) (.withDataType "String") (.withStringValue value))
      (number? value) (doto (MessageAttributeValue.) (.withDataType "Number") (.withStringValue (str value)))
      :else (throw (ex-info
                     (format "Values of type %s are not supported for SNS Message Attributes" (class value))
                     {:value value})))))

(amz/set-client AmazonSNSClient *ns*)
