(ns amazonica.aws.backup
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.backup.AWSBackupClient))

(amz/set-client AWSBackupClient *ns*)
