(ns amazonica.aws.medialive
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.medialive AWSMediaLiveClient]))

(amz/set-client AWSMediaLiveClient *ns*)
