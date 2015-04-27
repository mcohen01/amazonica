(ns amazonica.aws.ecs
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.ecs AmazonECS AmazonECSClient]))

(amz/set-client AmazonECS *ns*)
(amz/set-client AmazonECSClient *ns*)


