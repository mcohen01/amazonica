(ns amazonica.test.ecs
  (:require [amazonica.aws.ecs :refer :all]
            [clojure.set :as set]
            [clojure.test :refer :all]))


(deftest ecs-ns-should-contain
  (let [expected-functions ["adjust-client-configuration"
                            "create-cluster"
                            "create-service"
                            "delete-cluster"
                            "delete-service"
                            "deregister-container-instance"
                            "deregister-task-definition"
                            "describe-clusters"
                            "describe-container-instances"
                            "describe-services"
                            "describe-task-definition"
                            "describe-tasks"
                            "discover-poll-endpoint"
                            "list-clusters"
                            "list-container-instances"
                            "list-services"
                            "list-task-definition-families"
                            "list-task-definitions"
                            "list-tasks"
                            "register-container-instance"
                            "register-task-definition"
                            "run-task"
                            "set-region"
                            "shutdown"
                            "start-task"
                            "stop-task"
                            "submit-container-state-change"
                            "submit-task-state-change"
                            "update-service"]
        actual-functions (map (comp str first) (ns-publics 'amazonica.aws.ecs))]
    ;; actual functions should be a subset of the known functions
    (is (empty? (set/difference (set expected-functions) (set actual-functions))))))

