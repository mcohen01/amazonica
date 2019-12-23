(ns amazonica.aws.docdb
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.docdb.AmazonDocDBClient))

(amz/set-client AmazonDocDBClient *ns*)
