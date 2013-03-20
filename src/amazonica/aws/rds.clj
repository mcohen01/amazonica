(ns amazonica.aws.rds
  (:import com.amazonaws.services.rds.AmazonRDSClient))

(amazonica.core/set-client AmazonRDSClient *ns*)