(ns amazonica.aws.elasticloadbalancing
  (:import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient))

(amazonica.core/set-client AmazonElasticLoadBalancingClient *ns*)