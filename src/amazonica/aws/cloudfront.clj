(ns amazonica.aws.cloudfront
  (:import com.amazonaws.services.cloudfront.AmazonCloudFrontClient))

(amazonica.core/set-client AmazonCloudFrontClient *ns*)