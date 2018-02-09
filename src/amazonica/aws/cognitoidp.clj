(ns amazonica.aws.cognitoidp
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.cognitoidp AWSCognitoIdentityProviderClient]
           [com.amazonaws.services.cognitoidp.model AttributeType]))

(amz/register-coercions AttributeType
  (fn [[n v]]
    (-> (AttributeType.)
        (.withName n)
        (.withValue v))))

(amz/set-client AWSCognitoIdentityProviderClient *ns*)
