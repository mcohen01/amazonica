(ns amazonica.aws.waf
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.waf AWSWAFClient]))

(amz/set-client AWSWAFClient *ns*)
