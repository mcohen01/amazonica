(ns amazonica.aws.securitytoken
  (:use [amazonica.core :only (IMarshall marshall)])
  (:import [com.amazonaws.services.securitytoken
              AWSSecurityTokenServiceClient]
           [com.amazonaws.services.securitytoken.model
              Credentials]))
  

(extend-protocol IMarshall
  Credentials
  (marshall [obj]
    {:access-key    (.getAccessKeyId obj)
     :secret-key    (.getSecretAccessKey obj)
     :session-token (.getSessionToken obj)
     :expiration    (.getExpiration obj)}))
  
(amazonica.core/set-client AWSSecurityTokenServiceClient *ns*)