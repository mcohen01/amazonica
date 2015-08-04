(ns amazonica.aws.config
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.config AmazonConfigClient]))

(amz/set-client AmazonConfigClient *ns*)
