(ns amazonica.aws.cognitosync
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.cognitosync.AmazonCognitoSyncClient))

(amz/set-client AmazonCognitoSyncClient *ns*)
