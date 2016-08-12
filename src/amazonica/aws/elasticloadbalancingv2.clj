(ns amazonica.aws.elasticloadbalancingv2
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient))

(amz/set-client AmazonElasticLoadBalancingClient *ns*)
