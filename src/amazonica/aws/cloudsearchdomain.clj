(ns amazonica.aws.cloudsearchdomain
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient
           com.amazonaws.AmazonWebServiceClient))

(amz/set-client AmazonCloudSearchDomainClient *ns*)

(defn set-endpoint [& args]
  (.setEndpoint
    ^AmazonWebServiceClient (amz/candidate-client AmazonCloudSearchDomainClient args)
    (last args)))
