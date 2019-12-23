(ns amazonica.aws.connect
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.connect.AmazonConnectClient))

(amz/set-client AmazonConnectClient *ns*)
