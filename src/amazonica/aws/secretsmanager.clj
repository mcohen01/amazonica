(ns amazonica.aws.secretsmanager
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.secretsmanager AWSSecretsManagerClient]))

(amz/set-client AWSSecretsManagerClient *ns*)
