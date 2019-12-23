(ns amazonica.aws.apigatewaymanagementapi
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.apigatewaymanagementapi.AmazonApiGatewayManagementApiClient))

(amz/set-client AmazonApiGatewayManagementApiClient *ns*)
