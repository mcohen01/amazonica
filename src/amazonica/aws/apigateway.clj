(ns amazonica.aws.apigateway
  (:refer-clojure :exclude [get-method])
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.apigateway.AmazonApiGatewayClient))

(amz/set-client AmazonApiGatewayClient *ns*)
