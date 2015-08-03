(ns amazonica.aws.directory
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.directory AWSDirectoryServiceClient]))

(amz/set-client AWSDirectoryServiceClient *ns*)
