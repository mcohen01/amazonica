(ns amazonica.aws.cloudsearchdomain
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient))

(amz/set-client AmazonCloudSearchDomainClient *ns*)

(defn set-endpoint [& args]
  (.setEndpoint
    (amz/candidate-client AmazonCloudSearchDomainClient args)
    (last args)))