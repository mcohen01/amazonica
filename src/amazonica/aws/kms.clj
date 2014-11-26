(ns amazonica.aws.kms
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.kms.AWSKMSClient))

(amz/set-client AWSKMSClient *ns*)
