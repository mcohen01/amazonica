(ns amazonica.aws.iotdata
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.iotdata AWSIotDataClient]))

(amz/set-client AWSIotDataClient *ns*)
