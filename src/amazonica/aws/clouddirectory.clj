(ns amazonica.aws.clouddirectory
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.clouddirectory AmazonCloudDirectoryClient]))

(amz/set-client AmazonCloudDirectoryClient *ns*)
