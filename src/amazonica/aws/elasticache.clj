(ns amazonica.aws.elasticache
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.elasticache.AmazonElastiCacheClient))

(amz/set-client AmazonElastiCacheClient *ns*)