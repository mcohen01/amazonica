(ns amazonica.aws.qldb
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.qldb.AmazonQLDBClient))

(amz/set-client AmazonQLDBClient *ns*)
