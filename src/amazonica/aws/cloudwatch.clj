(ns amazonica.aws.cloudwatch
  (:import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient))

(amazonica.core/set-client AmazonCloudWatchClient *ns*)