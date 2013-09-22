(ns amazonica.aws.route53
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.route53.AmazonRoute53Client))

(amz/set-client AmazonRoute53Client *ns*)