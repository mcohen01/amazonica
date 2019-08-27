(ns amazonica.aws.ec2instanceconnect
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.ec2instanceconnect.AWSEC2InstanceConnectClient))

(amz/set-client AWSEC2InstanceConnectClient *ns*)
