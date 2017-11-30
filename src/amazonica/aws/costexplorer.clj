(ns amazonica.aws.costexplorer
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.costexplorer AWSCostExplorerClient]))

(amz/set-client AWSCostExplorerClient *ns*)
