(ns amazonica.aws.resourcegroups
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.resourcegroups.AWSResourceGroupsClient))

(amz/set-client AWSResourceGroupsClient *ns*)
