(ns amazonica.aws.cloudwatch
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient))

(amz/set-client AmazonCloudWatchClient *ns*)