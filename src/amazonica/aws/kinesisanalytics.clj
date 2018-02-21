(ns amazonica.aws.kinesisanalytics
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.kinesisanalytics.AmazonKinesisAnalyticsClient))

(amz/set-client AmazonKinesisAnalyticsClient *ns*)
