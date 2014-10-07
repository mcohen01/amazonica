(ns amazonica.aws.cognitoidentity
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient))

(amz/set-client AmazonCognitoIdentityClient *ns*)
