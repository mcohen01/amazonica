(ns amazonica.aws.lakeformation
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.lakeformation.AWSLakeFormationClient))

(amz/set-client AWSLakeFormationClient *ns*)
