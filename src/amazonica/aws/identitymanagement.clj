(ns amazonica.aws.identitymanagement
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient))

(amz/set-client AmazonIdentityManagementClient *ns*)