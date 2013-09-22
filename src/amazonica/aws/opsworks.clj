(ns amazonica.aws.opsworks
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.opsworks.AWSOpsWorksClient))

(amz/set-client AWSOpsWorksClient *ns*)