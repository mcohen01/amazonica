(ns amazonica.aws.simpleemail
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient
           com.amazonaws.services.simpleemail.model.Content))

(amz/register-coercions
  Content
  (fn [value]
    (if (coll? value)
        (Content. (:data value))
        (Content. value))))

(amz/set-client AmazonSimpleEmailServiceClient *ns*)