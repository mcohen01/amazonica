(ns amazonica.aws.databasemigrationservice
  (:require [amazonica.core :as amz])
  (:import [com.amazonaws.services.databasemigrationservice AWSDatabaseMigrationServiceClient]))

(amz/set-client AWSDatabaseMigrationServiceClient *ns*)
