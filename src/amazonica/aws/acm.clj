(ns amazonica.aws.acm
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.certificatemanager.AWSCertificateManagerClient))

(amz/set-client AWSCertificateManagerClient *ns*)
