(ns amazonica.aws.simpledb
  (:import com.amazonaws.services.simpledb.AmazonSimpleDBClient))

(amazonica.core/set-client AmazonSimpleDBClient *ns*)