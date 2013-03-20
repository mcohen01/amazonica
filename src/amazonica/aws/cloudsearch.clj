(ns amazonica.aws.cloudsearch
  (:import com.amazonaws.services.cloudsearch.AmazonCloudSearchClient))

(amazonica.core/set-client AmazonCloudSearchClient *ns*)