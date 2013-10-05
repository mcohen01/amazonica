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

  (def file "hw.txt")
  (def down-dir (java.io.File. (str "/tmp/" file)))
  (def bucket "pupuserious")
  
  (import 'java.security.KeyPairGenerator)
  (import 'java.security.SecureRandom)
  (def key-pair
    (let [kg (KeyPairGenerator/getInstance "RSA")]
      (.initialize kg 1024 (SecureRandom.))
      (.generateKeyPair kg)))

  (let [upl (upload :bucket-name bucket
                    :key file
                    :encryption {:key-pair key-pair}
                    :file down-dir)]
    ((:add-progress-listener upl) #(println %)))
  
  (let [dl  (download :bucket-name bucket
                      ;:encryption {:key-pair key-pair}
                      :key file
                      down-dir)
        listener #(if (= :completed (:event %))
                      (println ((:object-metadata dl)))
                      (println %))]
    ((:add-progress-listener dl) listener)))
