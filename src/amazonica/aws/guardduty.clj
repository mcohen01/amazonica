(ns amazonica.aws.guardduty
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.guardduty AmazonGuardDutyClient]))

(amz/set-client AmazonGuardDutyClient *ns*)
