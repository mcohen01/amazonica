(ns amazonica.aws.dataexchange
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.dataexchange.AWSDataExchangeClient))

(amz/set-client AWSDataExchangeClient *ns*)
