(ns amazonica.aws.frauddetector
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.frauddetector.AmazonFraudDetectorClient))

(amz/set-client AmazonFraudDetectorClient *ns*)
