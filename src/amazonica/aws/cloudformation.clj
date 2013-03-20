(ns amazonica.aws.cloudformation
  (:import com.amazonaws.services.cloudformation.AmazonCloudFormationClient))

(amazonica.core/set-client AmazonCloudFormationClient *ns*)