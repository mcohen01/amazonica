(ns amazonica.aws.ec2
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.ec2.AmazonEC2Client))

(amz/set-client AmazonEC2Client *ns*)