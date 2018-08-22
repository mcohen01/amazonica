(ns amazonica.aws.iotanalytics
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.iotanalytics AWSIoTAnalyticsClient]))

(amz/set-client AWSIoTAnalyticsClient *ns*)
