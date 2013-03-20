(ns amazonica.aws.route53
  (:import com.amazonaws.services.route53.AmazonRoute53Client))

(amazonica.core/set-client AmazonRoute53Client *ns*)