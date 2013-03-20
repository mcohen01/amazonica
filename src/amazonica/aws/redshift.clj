(ns amazonica.aws.redshift
  "Amazon Redshift support."  
  (:import com.amazonaws.services.redshift.AmazonRedshiftClient))

(amazonica.core/set-client AmazonRedshiftClient *ns*)