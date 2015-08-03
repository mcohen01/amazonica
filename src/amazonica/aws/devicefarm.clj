(ns amazonica.aws.devicefarm
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.devicefarm AWSDeviceFarmClient]))

(amz/set-client AWSDeviceFarmClient *ns*)
