(ns amazonica.aws.mediastore
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.mediastore AWSMediaStoreClient]))

(amz/set-client AWSMediaStoreClient *ns*)
