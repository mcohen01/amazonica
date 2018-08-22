(ns amazonica.aws.fms
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.fms AWSFMSClient]))

(amz/set-client AWSFMSClient *ns*)
