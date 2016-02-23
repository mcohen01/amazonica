(ns amazonica.aws.simplesystemsmanagement
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient))

(amz/set-client AWSSimpleSystemsManagementClient *ns*)
