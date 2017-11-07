(ns amazonica.aws.route53domains
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.route53domains.AmazonRoute53DomainsClient))

(amz/set-client AmazonRoute53DomainsClient *ns*)
