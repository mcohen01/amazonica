(ns amazonica.aws.opsworks
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.opsworks.AWSOpsWorksClient
           com.amazonaws.services.opsworks.model.DeploymentCommand
           com.amazonaws.services.opsworks.model.DeploymentCommandName))

(amz/register-coercions
  DeploymentCommand
  (fn [value]
    (doto (DeploymentCommand.)
      (.setName ^DeploymentCommandName (:name value))
      (.setArgs (:args value)))))

(amz/set-client AWSOpsWorksClient *ns*)
