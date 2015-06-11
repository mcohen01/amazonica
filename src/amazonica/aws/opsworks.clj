(ns amazonica.aws.opsworks
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.opsworks.AWSOpsWorksClient
           com.amazonaws.services.opsworks.model.DeploymentCommand))

(amz/register-coercions
  DeploymentCommand
  (fn [value]
    (doto (DeploymentCommand.)
      (.setName (:name value))
      (.setArgs (:args value)))))

(amz/set-client AWSOpsWorksClient *ns*)