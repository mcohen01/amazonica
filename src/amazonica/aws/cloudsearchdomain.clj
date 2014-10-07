(ns amazonica.aws.cloudsearchdomain
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient))

(amz/set-client AmazonCloudSearchDomainClient *ns*)
