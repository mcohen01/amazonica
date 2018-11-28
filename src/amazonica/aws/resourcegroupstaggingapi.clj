(ns amazonica.aws.resourcegroupstaggingapi
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.resourcegroupstaggingapi.AWSResourceGroupsTaggingAPIClient))

(amz/set-client AWSResourceGroupsTaggingAPIClient *ns*)
