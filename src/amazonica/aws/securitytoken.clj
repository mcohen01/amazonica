(ns amazonica.aws.securitytoken
  "Amazon Security Token support."
  (:import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient))

(amazonica.core/set-client AWSSecurityTokenServiceClient *ns*)