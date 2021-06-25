(ns amazonica.aws.cloudfront
  (:require [amazonica.core])
  (:import com.amazonaws.services.cloudfront.AmazonCloudFrontClient))

(amazonica.core/set-client AmazonCloudFrontClient *ns*)
