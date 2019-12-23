(ns amazonica.aws.acmpca
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.acmpca.AWSACMPCAClient))

(amz/set-client AWSACMPCAClient *ns*)
