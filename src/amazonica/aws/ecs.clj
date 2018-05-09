(ns amazonica.aws.ecs
  (:require [amazonica.core :as amz]
            [clojure.walk :as walk])
  (:import [com.amazonaws.services.ecs AmazonECSClient]
           (com.amazonaws.services.ecs.model LogConfiguration LogDriver)))

(amz/set-client AmazonECSClient *ns*)

(amz/register-coercions
  LogConfiguration
  (fn [value]
    (doto (LogConfiguration.)
      (.setLogDriver ^LogDriver (:log-driver value))
      (.setOptions (walk/stringify-keys (:options value))))))

