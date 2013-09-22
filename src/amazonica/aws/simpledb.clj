(ns amazonica.aws.simpledb
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.simpledb.AmazonSimpleDBClient))

(amz/set-client AmazonSimpleDBClient *ns*)