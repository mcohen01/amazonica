(ns amazonica.aws.forecastquery
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.forecastquery AmazonForecastQueryClient]))

(amz/set-client AmazonForecastQueryClient *ns*)
