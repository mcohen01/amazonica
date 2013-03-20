(ns amazonica.aws.elasticache
  (:import com.amazonaws.services.elasticache.AmazonElastiCacheClient))

(amazonica.core/set-client AmazonElastiCacheClient *ns*)