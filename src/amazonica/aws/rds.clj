(ns amazonica.aws.rds
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.rds.AmazonRDSClient))

(amz/set-client AmazonRDSClient *ns*)