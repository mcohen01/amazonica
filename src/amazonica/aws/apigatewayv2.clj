(ns amazonica.aws.apigatewayv2
  (:refer-clojure :exclude [get-method])
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.apigatewayv2.AmazonApiGatewayV2Client))

(amz/set-client AmazonApiGatewayV2Client *ns*)
