(ns amazonica.aws.iot
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.iot AWSIotClient]))

(amz/set-client AWSIotClient *ns*)
