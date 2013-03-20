(ns amazonica.aws.simpleemail
  (:import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient))

(amazonica.core/set-client AmazonSimpleEmailServiceClient *ns*)