(ns amazonica.test.simplesystemsmanagement
  (:require
   [clojure.test :refer :all]
   [amazonica.aws.simplesystemsmanagement :refer :all])
  (:import
   (com.amazonaws.services.simplesystemsmanagement.model SendCommandRequest)))

(deftest simplesystemsmanagement []

  ;; test for marshalling map values
  ;; see https://github.com/mcohen01/amazonica/issues/219
  (let [pojo (SendCommandRequest.)]
    (amazonica.core/set-fields pojo {:parameters {"k" ["one" "two"]}})
    (is (= {"k" ["one" "two"]}
           (.getParameters pojo)))))
