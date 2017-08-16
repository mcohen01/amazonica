(ns amazonica.aws.cognitoidp
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.cognitoidp AWSCognitoIdentityProviderClient]))

(amz/set-client AWSCognitoIdentityProviderClient *ns*)
