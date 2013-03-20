(ns amazonica.aws.elasticmapreduce
  (:import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient))

(amazonica.core/set-client AmazonElasticMapReduceClient *ns*)