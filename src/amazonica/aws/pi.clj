(ns amazonica.aws.pi
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.pi AWSPIClient]))

(amz/set-client AWSPIClient *ns*)
