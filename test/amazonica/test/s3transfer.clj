(ns amazonica.test.s3transfer
  (:use [amazonica.core]
        [amazonica.aws.s3transfer]
        [clojure.test]))

(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))


(deftest s3transfer []

  (def file "big-jar.jar")
  (def down-dir (java.io.File. (str "/tmp/" file)))
  (def bucket "my-bucket")
  
  (let [upl (upload cred
                    bucket
                    file
                    down-dir)]
    ((:add-progress-listener upl)
      (new-progress-listener #(println %))))
  
  (let [dl  (download cred
                      bucket
                      file
                      down-dir)
        listener #(if (= :completed (:event %))
                      (do
                        (println ((:object-metadata dl)))
                        (is (= bucket ((:bucket-name dl))))
                        (is (= file ((:key dl)))))
                      (println %))]
    ((:add-progress-listener dl)
      (new-progress-listener listener))))
