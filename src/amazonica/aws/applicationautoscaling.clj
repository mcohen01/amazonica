(ns amazonica.aws.applicationautoscaling
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.applicationautoscaling.AWSApplicationAutoScalingClient))

(amz/set-client AWSApplicationAutoScalingClient *ns*)
