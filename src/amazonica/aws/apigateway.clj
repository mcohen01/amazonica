(ns amazonica.aws.apigateway
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.apigateway.AmazonApiGatewayClient))

(amz/set-client AmazonApiGatewayClient *ns*)
