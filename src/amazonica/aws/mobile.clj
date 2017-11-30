(ns amazonica.aws.mobile
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.mobile AWSMobileClient]))

(amz/set-client AWSMobileClient *ns*)
