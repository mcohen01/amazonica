(ns amazonica.aws.gamelift
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.gamelift AmazonGameLiftClient]))

(amz/set-client AmazonGameLiftClient *ns*)
