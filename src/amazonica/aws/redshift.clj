(ns amazonica.aws.redshift
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.redshift.AmazonRedshiftClient))

(amz/set-client AmazonRedshiftClient *ns*)