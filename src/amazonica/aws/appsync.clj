(ns amazonica.aws.appsync
  (:refer-clojure :exclude [get-method])
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.appsync.AWSAppSyncClient))

(amz/set-client AWSAppSyncClient *ns*)
