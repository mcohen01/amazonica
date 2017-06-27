(ns amazonica.aws.lexruntime
  (:refer-clojure :exclude [get-method])
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.lexruntime.AmazonLexRuntimeClient))

(amz/set-client AmazonLexRuntimeClient *ns*)
