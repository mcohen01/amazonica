(ns amazonica.aws.mediapackage
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.mediapackage AWSMediaPackageClient]))

(amz/set-client AWSMediaPackageClient *ns*)
