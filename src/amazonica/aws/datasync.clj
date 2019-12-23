(ns amazonica.aws.datasync
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.datasync.AWSDataSyncClient))

(amz/set-client AWSDataSyncClient *ns*)
