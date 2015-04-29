(ns amazonica.aws.ecs
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.ecs AmazonECSClient]))

(amz/set-client AmazonECSClient *ns*)
