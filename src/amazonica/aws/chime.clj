(ns amazonica.aws.chime
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.chime.AmazonChimeClient))

(amz/set-client AmazonChimeClient *ns*)
