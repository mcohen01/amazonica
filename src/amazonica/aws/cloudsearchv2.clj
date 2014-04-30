(ns amazonica.aws.cloudsearchv2
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.cloudsearchv2.AmazonCloudSearchClient))

(amz/set-client AmazonCloudSearchClient *ns*)
