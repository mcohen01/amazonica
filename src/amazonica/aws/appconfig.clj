(ns amazonica.aws.appconfig
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.appconfig.AmazonAppConfigClient))

(amz/set-client AmazonAppConfigClient *ns*)
