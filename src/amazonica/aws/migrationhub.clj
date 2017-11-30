(ns amazonica.aws.migrationhub
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.migrationhub AWSMigrationHubClient]))

(amz/set-client AWSMigrationHubClient *ns*)
