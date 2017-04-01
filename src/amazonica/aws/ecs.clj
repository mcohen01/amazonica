(ns amazonica.aws.ecs
  (:require [amazonica.core :as amz]
            [clojure.walk :as walk])
  (:import [com.amazonaws.services.ecs AmazonECSClient]
           (com.amazonaws.services.ecs.model LogConfiguration)))

(amz/set-client AmazonECSClient *ns*)

(amz/register-coercions
  LogConfiguration
  (fn [value]
    (doto (LogConfiguration.)
      (.setLogDriver (:log-driver value))
      (.setOptions (walk/stringify-keys (:options value))))))

