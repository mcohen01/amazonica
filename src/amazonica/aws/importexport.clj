(ns amazonica.aws.importexport
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.importexport AmazonImportExportClient]))

(amz/set-client AmazonImportExportClient *ns*)
