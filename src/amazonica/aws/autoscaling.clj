(ns amazonica.aws.autoscaling
  (:import com.amazonaws.services.autoscaling.AmazonAutoScalingClient))

(amazonica.core/set-client AmazonAutoScalingClient *ns*)