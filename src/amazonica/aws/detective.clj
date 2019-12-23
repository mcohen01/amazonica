(ns amazonica.aws.detective
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.detective.AmazonDetectiveClient))

(amz/set-client AmazonDetectiveClient *ns*)
