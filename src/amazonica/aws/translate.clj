(ns amazonica.aws.translate
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.translate.AmazonTranslateClient))

(amz/set-client AmazonTranslateClient *ns*)
