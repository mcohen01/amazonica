(ns amazonica.aws.cloudhsmv2
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.cloudhsmv2.AWSCloudHSMV2Client))

(amz/set-client AWSCloudHSMV2Client *ns*)
