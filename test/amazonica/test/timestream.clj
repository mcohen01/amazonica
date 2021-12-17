(ns amazonica.test.timestream
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.aws.timestreamwrite]))

(deftest timestream-write
  (clojure.pprint/pprint (list-tables)))
