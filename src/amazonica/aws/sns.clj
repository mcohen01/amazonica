(ns amazonica.aws.sns
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.sns.AmazonSNSClient))

(amz/set-client AmazonSNSClient *ns*)