(ns amazonica.aws.lexmodelbuilding
  (:refer-clojure :exclude [get-method])
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuildingClient))

(amz/set-client AmazonLexModelBuildingClient *ns*)
