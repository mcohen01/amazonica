(ns amazonica.aws.xray
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.xray AWSXRayClient]))

(amz/set-client AWSXRayClient *ns*)
