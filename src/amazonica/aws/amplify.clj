(ns amazonica.aws.amplify
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.amplify.AWSAmplifyClient))

(amz/set-client AWSAmplifyClient *ns*)
