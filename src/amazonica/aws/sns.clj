(ns amazonica.aws.sns
  (:import com.amazonaws.services.sns.AmazonSNSClient))

(amazonica.core/set-client AmazonSNSClient *ns*)