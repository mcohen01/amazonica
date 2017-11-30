(ns amazonica.aws.mq
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.mq AmazonMQClient]))

(amz/set-client AmazonMQClient *ns*)
