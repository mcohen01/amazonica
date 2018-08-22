(ns amazonica.aws.transcribe
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.transcribe AmazonTranscribeClient]))

(amz/set-client AmazonTranscribeClient *ns*)
