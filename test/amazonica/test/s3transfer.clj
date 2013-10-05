(ns amazonica.test.s3transfer
  (:use [amazonica.aws.s3transfer]
        [clojure.test]))

(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))


(deftest s3transfer []

  (def file "some-big.jar")
  (def down-dir (java.io.File. (str "/tmp/" file)))
  (def bucket "my-bucket")
  
  (let [upl (upload cred
                    bucket
                    file
                    down-dir)]
    ((:add-progress-listener upl) #(println %)))
  
  (let [dl  (download cred
                      bucket
                      file
                      down-dir)
        listener #(if (= :completed (:event %))
                      (println ((:object-metadata dl)))
                      (println %))]
    ((:add-progress-listener dl) listener)))
