(ns amazonica.aws.simpleemail
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient))

(amz/set-client AmazonSimpleEmailServiceClient *ns*)