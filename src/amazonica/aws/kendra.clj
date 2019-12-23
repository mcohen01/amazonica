(ns amazonica.aws.kendra
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.kendra.AWSkendraClient))

(amz/set-client AWSkendraClient *ns*)
