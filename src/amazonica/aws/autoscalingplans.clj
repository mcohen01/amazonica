(ns amazonica.aws.autoscalingplans
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.autoscalingplans.AWSAutoScalingPlansClient))

(amz/set-client AWSAutoScalingPlansClient *ns*)
