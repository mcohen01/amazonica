(ns amazonica.aws.shield
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.shield AWSShieldClient]))

(amz/set-client AWSShieldClient *ns*)
