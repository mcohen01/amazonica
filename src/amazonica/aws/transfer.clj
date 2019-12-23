(ns amazonica.aws.transfer
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.transfer.AWSTransferClient))

(amz/set-client AWSTransferClient *ns*)
