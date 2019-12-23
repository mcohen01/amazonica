(ns amazonica.aws.comprehendmedical
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.comprehendmedical.AWSComprehendMedicalClient))

(amz/set-client AWSComprehendMedicalClient *ns*)
