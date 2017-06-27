(ns amazonica.aws.athena
  (:refer-clojure :exclude [get-method])
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.athena.AmazonAthenaClient))

(amz/set-client AmazonAthenaClient *ns*)
