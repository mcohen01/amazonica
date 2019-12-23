(ns amazonica.aws.accessanalyzer
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.accessanalyzer.AWSAccessAnalyzerClient))

(amz/set-client AWSAccessAnalyzerClient *ns*)
