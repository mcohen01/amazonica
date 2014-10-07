(ns amazonica.aws.logs
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.logs.AWSLogsClient))

(amz/set-client AWSLogsClient *ns*)
