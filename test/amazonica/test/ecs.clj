(ns amazonica.test.ecs
  (:require [amazonica.aws.ecs :refer :all]
            [amazonica.aws.identitymanagement :as iam]
            [clojure.set :as set]
            [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]))

(deftest create-ecs-service
  (register-task-definition
   {:family "grafana2",
    :container-definitions [
                            {:name "grafana2"
                             :image "bbinet/grafana2",
                             :port-mappings [{:container-port 3000, :host-port 3000}]
                             :memory 300
                             :cpu 300
                             }]})
  (pprint (describe-task-definition :task-definition "grafana2"))
  (pprint (list-task-definitions :family-prefix "grafana2"))

  ;; create cluster 
  (create-cluster :cluster-name "Amazonica")

  (pprint (list-clusters))
  (pprint (describe-clusters))

  (create-service :cluster "Amazonica"
                  :service-name "grafana2"
                  :task-definition "grafana2" :desired-count 1
                  ;;:role "ecsServiceRole"
                  ;;:load-balancers ...
                  )
  (pprint (list-services :cluster "Amazonica"))
  (pprint (describe-services :cluster "Amazonica" :services ["grafana2"]))

  (update-service :cluster "Amazonica" :service "grafana2" :desired-count 0)
  (delete-service :cluster "Amazonica" :service "grafana2")
  (delete-cluster :cluster "Amazonica")  

  ;; at time of writing aws docs state this method is not yet implemented
  #_(deregister-task-definition :task-definition "grafana2"))



