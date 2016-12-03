(ns amazonica.aws.health
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.health AWSHealthClient]))

(amz/set-client AWSHealthClient *ns*)
