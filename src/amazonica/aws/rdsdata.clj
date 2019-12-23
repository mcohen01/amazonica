(ns amazonica.aws.rdsdata
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.rdsdata.AWSRDSDataClient))

(amz/set-client AWSRDSDataClient *ns*)
