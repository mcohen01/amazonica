(ns amazonica.aws.cloudsearch
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.cloudsearch.AmazonCloudSearchClient))

(amz/set-client AmazonCloudSearchClient *ns*)