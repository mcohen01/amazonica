(ns amazonica.aws.cloudtrail
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.cloudtrail.AWSCloudTrailClient))

(amz/set-client AWSCloudTrailClient *ns*)
