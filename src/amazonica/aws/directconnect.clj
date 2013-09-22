(ns amazonica.aws.directconnect
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.directconnect.AmazonDirectConnectClient))

(amz/set-client AmazonDirectConnectClient *ns*)