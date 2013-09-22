(ns amazonica.aws.elasticmapreduce
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient))

(amz/set-client AmazonElasticMapReduceClient *ns*)