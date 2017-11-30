(ns amazonica.aws.costandusagereport
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.costandusagereport AWSCostAndUsageReportClient]))

(amz/set-client AWSCostAndUsageReportClient *ns*)
