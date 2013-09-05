(ns amazonica.test.s3
  (:import org.joda.time.DateTime
           java.io.BufferedInputStream
           java.io.File
           java.io.FileInputStream
           java.text.SimpleDateFormat
           java.util.Date
           java.util.UUID)
  (:require [clojure.string :as str])
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.core]
        [amazonica.aws.s3]))

; config file contains space-separated AWS credential key pair
; and optional third param of AWS endpoint (e.g. for different
; region than the default US_East)
(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))

(deftest s3 []

  (def bucket1 (.. (UUID/randomUUID) toString))
  (def bucket2 (.. (UUID/randomUUID) toString))
  (def date    (.plusDays (DateTime.) 2))
  (def upload-file   (java.io.File. "upload.txt"))
  (def download-file (java.io.File. "download.txt"))

  (.createNewFile upload-file)
  (spit upload-file (Date.))  

  (try
    (abort-multipart-upload cred 
                            :bucket-name "some-bucket"
                            :key "some-key"
                            :upload-id "my-upload")
    (catch Exception e
      (is (.startsWith
            (:message (ex->map e)) 
            "The specified upload does not exist."))))  

  (try
    (complete-multipart-upload cred 
                               :bucket-name "some-bucket"
                               :key "some-key"
                               :upload-id "my-upload"
                               :part-etags [
                                 {:part-number 3
                                  :etag "my-etag"}])
    (catch Exception e
      (is (.startsWith
            (:message (ex->map e)) 
            "The specified upload does not exist."))))
  
  (create-bucket cred bucket1)
  (delete-bucket cred bucket1)
  
  (def bucket1 (.. (UUID/randomUUID) toString))
  (create-bucket cred 
                 :region "us-west-1"
                 :bucket-name bucket1)  
  (create-bucket cred bucket2 "us-west-1")

  (put-object cred bucket1 "jenny" upload-file)

  (copy-object
    cred 
    :source-bucket-name bucket1
    :destination-bucket-name bucket2
    :source-key "jenny" 
    :destination-key "jenny" 
    :new-object-metadata 
      {:content-type "text/html" 
       :user-metadata 
         {:foo "bar"
          :baz "barry"}})

  (is (= {:foo "bar"
          :baz "barry"}
        (get-in
          (get-object cred bucket2 "jenny")
          [:object-metadata :user-metadata])))
  
  (is (= (get-in
          (get-object-acl cred bucket1 "jenny")
          [:grants 0 :permission :header-name])
         "x-amz-grant-full-control"))        

  (put-object cred
             :bucket-name bucket1
             :key "jenny"
             :file upload-file
             :access-control-list
               {:grant-permission ["AllUsers" "Read"]})

  (is (= (get-in
           (get-object-acl cred bucket1 "jenny")
           [:grants 0 :permission :header-name])
         "x-amz-grant-read"))

  (put-object cred
             :bucket-name bucket1
             :key "jenny"
             :file upload-file
             :access-control-list {
               :grant-all [
                 ["AllUsers" "Read"]
                 ["AuthenticatedUsers" "Write"]]})

  (let [obj (get-object-acl cred bucket1 "jenny")
        f   #(fn [{{p :header-name} :permission}]
               (= p %))]
    (is (= 2 (count (:grants obj))))
    (is 
      (->
        (f "x-amz-grant-read")
        (filter (:grants obj))
        first
        (get-in [:grantee :identifier])
        (.contains "AllUsers")))
    (is 
      (->
        (f "x-amz-grant-write")
        (filter (:grants obj))
        first
        (get-in [:grantee :identifier])
        (.contains "AuthenticatedUsers"))))

  (put-object cred
             :bucket-name bucket1
             :key "jenny"
             :file upload-file
             :access-control-list
               {:revoke-all-permissions "AllUsers"})

  (let [obj (get-object-acl cred bucket1 "jenny")]
    (is (= 1 (count (:grants obj)))))
  
  (clojure.pprint/pprint
    (list-objects cred bucket1))

  (clojure.pprint/pprint
    (get-object-acl cred bucket1 "jenny"))



  (copy-object cred 
               :source-bucket-name bucket1
               :source-key "jenny" 
               :destination-bucket-name bucket2
               :destination-key "jenny")
  
  (copy-object cred bucket1 "jenny" bucket2 "jenny")

  (change-object-storage-class 
      cred bucket1 "jenny" "REDUCED_REDUNDANCY")

  (let [config {:rules [{:id "rm after 14 days"
                         :expiration-in-days 14
                         :prefix "some-prefix/"
                         :status "Enabled"}]}]
    (set-bucket-lifecycle-configuration cred bucket1 config)
    (is (= config (get-bucket-lifecycle-configuration cred bucket1))))

  (delete-bucket-cross-origin-configuration cred bucket1)
  (delete-bucket-lifecycle-configuration cred bucket1)
  (delete-bucket-policy cred bucket1)
  (delete-bucket-tagging-configuration cred bucket1)
  (delete-bucket-website-configuration cred bucket1)
  
  (get-s3account-owner cred)

  (list-buckets cred)
  
  (does-bucket-exist cred bucket1)

  (generate-presigned-url cred bucket1 "jenny" date)

  (get-bucket-acl cred bucket1)

  (get-bucket-location cred bucket1)

  (get-bucket-policy cred bucket1)

  (get-bucket-website-configuration cred bucket1)

  (get-bucket-website-configuration
    cred 
    :bucket-name bucket1)
  
  (list-objects cred bucket1)
  (list-objects cred bucket1 "")
  (list-objects cred :bucket-name bucket1)
  
  (clojure.pprint/pprint
    (get-object cred bucket1 "jenny"))

  (let [obj (get-object cred bucket1 "jenny" )
        in (:input-stream obj)]
    (clojure.java.io/copy in download-file))

  (is (= (slurp upload-file)
         (slurp download-file)))          

  (let [etag (put-object cred
                :bucket-name bucket1
                :key "jenny"
                :file upload-file)]
    (is 32 (.length (:etag etag))))

  
  (is "text/plain" 
    (get-in (get-object cred bucket1 "jenny")
            [:object-metadata :raw-metadata :Content-Type]))

  (get-object
    cred
    :bucket-name bucket1
    :key "jenny")

  (get-object cred bucket1 "jenny")
  
  (copy-object cred bucket1 "jenny" bucket2 "jenny")
    

  (generate-presigned-url cred bucket1 "jenny" date)
  (generate-presigned-url cred bucket2 "jenny" date "POST")

  (delete-object cred bucket1 "jenny")
  (delete-object cred bucket2 "jenny")

  (delete-bucket cred bucket1)
  (delete-bucket cred bucket2)

  (if (.exists download-file)
    (.delete download-file))

)