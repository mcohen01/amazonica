(ns amazonica.aws.servicecatalog
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.servicecatalog AWSServiceCatalogClient]))

(amz/set-client AWSServiceCatalogClient *ns*)
