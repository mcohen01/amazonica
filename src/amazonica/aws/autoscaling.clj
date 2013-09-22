(ns amazonica.aws.autoscaling
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.autoscaling.AmazonAutoScalingClient))

(amz/set-client AmazonAutoScalingClient *ns*)