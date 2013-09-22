(ns amazonica.aws.cloudformation
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.cloudformation.AmazonCloudFormationClient))

(amz/set-client AmazonCloudFormationClient *ns*)