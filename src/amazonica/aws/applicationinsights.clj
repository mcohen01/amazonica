(ns amazonica.aws.applicationinsights
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.applicationinsights.AmazonApplicationInsightsClient))

(amz/set-client AmazonApplicationInsightsClient *ns*)
