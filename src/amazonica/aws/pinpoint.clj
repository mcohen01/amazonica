(ns amazonica.aws.pinpoint
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.pinpoint AmazonPinpointClient]))

(amz/set-client AmazonPinpointClient *ns*)