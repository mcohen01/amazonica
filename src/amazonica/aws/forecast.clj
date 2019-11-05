(ns amazonica.aws.forecast
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.forecast AmazonForecastClient]))

(amz/set-client AmazonForecastClient *ns*)
