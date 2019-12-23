(ns amazonica.aws.eventbridge
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.eventbridge.AmazonEventBridgeClient))

(amz/set-client AmazonEventBridgeClient *ns*)
