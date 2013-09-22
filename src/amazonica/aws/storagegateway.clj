(ns amazonica.aws.storagegateway
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.storagegateway.AWSStorageGatewayClient))

(amz/set-client AWSStorageGatewayClient *ns*)