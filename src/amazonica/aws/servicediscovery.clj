(ns amazonica.aws.servicediscovery
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.servicediscovery AWSServiceDiscoveryClient]))

(amz/set-client AWSServiceDiscoveryClient *ns*)
