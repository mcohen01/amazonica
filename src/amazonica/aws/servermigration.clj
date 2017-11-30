(ns amazonica.aws.servermigration
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.servermigration AWSServerMigrationClient]))

(amz/set-client AWSServerMigrationClient *ns*)
