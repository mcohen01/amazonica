(ns amazonica.aws.codedeploy
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.codedeploy.AmazonCodeDeployClient))

(amz/set-client AmazonCodeDeployClient *ns*)
