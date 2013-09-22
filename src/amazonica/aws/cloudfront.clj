(ns amazonica.aws.cloudfront
  (:require [amazonica.core :only set-client])
  (:import com.amazonaws.services.cloudfront.AmazonCloudFrontClient))

(amazonica.core/set-client AmazonCloudFrontClient *ns*)