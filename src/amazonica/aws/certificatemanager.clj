(ns amazonica.aws.certificatemanager
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.certificatemanager.AWSCertificateManagerClient))

(amz/set-client AWSCertificateManagerClient *ns*)
