(ns amazonica.test.s3transfer
  (:require [amazonica.core :refer [with-credential defcredential]]
            [amazonica.aws.s3 :as s3])
  (:use [amazonica.aws.s3transfer]
        [clojure.test]))

(def cred
  (let [access "aws_access_key_id = "
        secret "aws_secret_access_key = "
        file   "/.aws/credentials"
        creds  (-> "user.home"
                   System/getProperty
                   (str file)
                   slurp
                   (.split "\n"))
        key-for (fn [k] (-> (filter #(.startsWith % k) creds)
                            first
                            (.replace k "")))]
    {:access-key (key-for access)
     :secret-key (key-for secret)}))


(deftest s3transfer []

  (def file "upload.txt")
  (def down-dir (java.io.File. (str "/tmp/" file)))
  (def bucket "0a178f17-5593-480f-bcf0-cb10f7654b19")
  
  (s3/create-bucket bucket)
  
  (Thread/sleep 5000)
  
  (let [upl (upload cred
                    bucket
                    file
                    (java.io.File. file))]
    ((:add-progress-listener upl) #(println %)))
  
  (with-credential [(:access-key cred)
                    (:secret-key cred)
                    (:endpoint cred)]
    (upload bucket
            file
            (java.io.File. file)))
  
  (defcredential (:access-key cred)
                 (:secret-key cred)
                 (:endpoint cred))
  
  (upload bucket
          file
          (java.io.File. file))
  
  (let [dl  (download cred
                      bucket
                      file
                      down-dir)
        listener #(if (= :completed (:event %))
                      (println ((:object-metadata dl)))
                      (println %))]
    ((:add-progress-listener dl) listener))
  
  (s3/delete-object bucket file)
  (s3/delete-bucket bucket))