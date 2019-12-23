(ns amazonica.aws.securityhub
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.securityhub.AWSSecurityHubClient))

(amz/set-client AWSSecurityHubClient *ns*)
