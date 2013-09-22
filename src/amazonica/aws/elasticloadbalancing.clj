(ns amazonica.aws.elasticloadbalancing
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient))

(amz/set-client AmazonElasticLoadBalancingClient *ns*)
