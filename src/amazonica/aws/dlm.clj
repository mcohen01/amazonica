(ns amazonica.aws.dlm
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.dlm.AmazonDLMClient))

(amz/set-client AmazonDLMClient *ns*)
