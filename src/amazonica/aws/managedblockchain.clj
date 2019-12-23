(ns amazonica.aws.managedblockchain
  (:refer-clojure :exclude [get-method])
  (:require [amazonica.core :as amz])
  (:import com.amazonaws.services.managedblockchain.AmazonManagedBlockchainClient))

(amz/set-client AmazonManagedBlockchainClient *ns*)
