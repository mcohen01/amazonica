(ns amazonica.test.s3
  (:import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
           com.amazonaws.services.s3.model.CORSRule
           com.amazonaws.services.s3.model.ObjectListing
           org.joda.time.DateTime
           java.io.BufferedInputStream
           java.io.File
           java.io.FileInputStream
           java.text.SimpleDateFormat
           java.util.Date
           java.util.UUID
           java.security.KeyPairGenerator
           java.security.SecureRandom)
  (:require [clojure.string :as str])
  (:use [clojure.test]
        [clojure.set]
        [clojure.pprint]
        [amazonica.core]
        [amazonica.aws.s3]))

(def cred
  (let [access "aws_access_key_id"
        secret "aws_secret_access_key"
        file   "/.aws/credentials"
        creds  (-> "user.home"
                   System/getProperty
                   (str file)
                   slurp
                   (.split "\n"))]
    (clojure.set/rename-keys 
      (reduce
        (fn [m e]
          (let [pair (.split e "=")]
            (if (some #{access secret} [(first pair)])
                (apply assoc m pair)
                m)))
        {}
        creds)
      {access :access-key secret :secret-key})))

(deftest s3 []

  (def bucket1 (.. (UUID/randomUUID) toString))
  (def bucket2 (.. (UUID/randomUUID) toString))
  (def date    (.plusDays (DateTime.) 2))
  (def upload-file   (java.io.File. "upload.txt"))
  (def download-file (java.io.File. "download.txt"))

  (spit upload-file "hello world")

  (create-bucket bucket1)
  
  (list-buckets cred)
  
  (list-buckets (DefaultAWSCredentialsProviderChain.))

  ; (def uuid-regex #"[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")
  ; (reduce
  ;   (fn [m e]
  ;     (if (re-find uuid-regex (:name e))
  ;         (do
  ;           (println (:name e))
  ;           (reduce
  ;             (fn [mm ee]
  ;               (delete-object (:name e) (:key ee))
  ;               mm)
  ;             []
  ;             (:object-summaries 
  ;               (list-objects :bucket-name (:name e))))
  ;           (delete-bucket (:name e))))
  ;     m)
  ;   []
  ;   (list-buckets))
  
  (list-buckets)
  
  (defcredential (:access-key cred)
                 (:secret-key cred)
                 (:endpoint cred))
  
  (list-buckets)
  
  (with-credential [(:access-key cred)
                    (:secret-key cred)
                    (:endpoint cred)]
    (list-buckets)) 
  
  ;; test the various invocations   
  (list-objects bucket1)
  
  (list-objects :bucket-name bucket1 :prefix "")
  (list-objects {:bucket-name bucket1 :prefix ""})
  (list-objects cred :bucket-name bucket1 :prefix "")
  (list-objects cred {:bucket-name bucket1 :prefix ""})
  
  (list-objects :bucket-name bucket1)
  (list-objects {:bucket-name bucket1})
  (list-objects cred :bucket-name bucket1)
  (list-objects cred {:bucket-name bucket1})
    
  (def key-pair
    (let [kg (KeyPairGenerator/getInstance "RSA")]
      (.initialize kg 1024 (SecureRandom.))
      (.generateKeyPair kg)))    
  
  ;; encrypted upload
  (put-object :bucket-name bucket1
              :key "jenny"
              :encryption {:key-pair key-pair}
              :file upload-file)
  
  ;; UNencrypted download
  (is (not= "hello world"
            (slurp (:input-stream
              (get-object :bucket-name bucket1
                          :key "jenny")))))
  
  ;; encrypted download
  (is (= "hello world"
         (slurp (:input-stream
           (get-object :bucket-name bucket1
                       :encryption {:key-pair key-pair}
                       :key "jenny")))))
  
  ;; server-side, not client side encryption
  (put-object :bucket-name bucket1
              :key "jenny"
              :metadata {:server-side-encryption "AES256"}
              :file upload-file)
  
  ;; client side UNdecrypted, but server side decrypted download
  (is (= "hello world"
         (slurp (:input-stream
           (get-object :bucket-name bucket1
                       :key "jenny")))))
  
  

  ;; upload file with credentials and client options
  (put-object
   (assoc cred :client-config {:max-connections 1
                                :user-agent "Amazonica"})
   :bucket-name bucket1
   :key "creds-and-options"
   :file upload-file)

  (is (= "hello world"
         (slurp (:input-stream
                 (get-object :bucket-name bucket1
                             :key "creds-and-options")))))

  ;; upload file with just creds
  (put-object
   cred
   :bucket-name bucket1
   :key "creds"
   :file upload-file)

  (is (= "hello world"
         (slurp (:input-stream
                 (get-object :bucket-name bucket1
                             :key "creds")))))

  ;; upload file without credentials but with client options
  (put-object
   {:client-config {:max-connections 1
                    :user-agent "Amazonica"}}
   :bucket-name bucket1
   :key "options"
   :file upload-file)

  (is (= "hello world"
         (slurp (:input-stream
                 (get-object :bucket-name bucket1
                             :key "options")))))

  (.createNewFile upload-file)
  (spit upload-file (Date.))  

  (try
    (abort-multipart-upload cred 
                            :bucket-name "some-bucket"
                            :key "some-key"
                            :upload-id "my-upload")
    (catch Exception e
      (is (= (:error-code (ex->map e)) "NoSuchUpload"))))

  (try
    (complete-multipart-upload cred 
                               :bucket-name "some-bucket"
                               :key "some-key"
                               :upload-id "my-upload"
                               :part-etags [
                                 {:part-number 3
                                  :etag "my-etag"}])
    (catch Exception e
      (is (is (= (:error-code (ex->map e)) "NoSuchUpload")))))
  
  
  (delete-object bucket1 "jenny")
  (delete-object bucket1 "creds-and-options")
  (delete-object bucket1 "options")
  (delete-object bucket1 "creds")
  (delete-bucket bucket1)
  
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
                         :noncurrent-version-expiration-in-days -1
                         :prefix "some-prefix/"
                         :status "Enabled"}]}]
    (set-bucket-lifecycle-configuration cred bucket1 config)
    (is (= config (get-bucket-lifecycle-configuration cred bucket1))))

  (set-bucket-cross-origin-configuration cred bucket1 {:rules [{:allowed-methods ["GET" "POST"]
                                                                :allowed-origins ["*"]
                                                                :max-age-seconds 9000
                                                                :allowed-headers ["Authorization"]}]})
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
  
  (get-object
    cred
    :bucket-name bucket1
    :key "jenny"
    :range [0 100])

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

   
  ;; test for marshalling map values 
  ;; see https://github.com/mcohen01/amazonica/issues/219
  (let [pojo (CORSRule.)]
    (amazonica.core/set-fields pojo {:allowed-headers ["foo" "bar" "baz"]})
    (is (= ["foo" "bar" "baz"]
           (.getAllowedHeaders pojo))))
  
  (let [pojo (ObjectListing.)]
    (amazonica.core/set-fields pojo {:common-prefixes ["foo" "bar" "baz"]})
    (is (= ["foo" "bar" "baz"]
           (.getCommonPrefixes pojo))))

)

(deftest email-test []
  (are [x] (= x (re-find email-pattern x))
    "foo@bar.com"
    "foo@bar.co.jp")
  (are [x] (nil? (re-find email-pattern x))
    "foo"
    "foo@"
    "foo@bar"
    "foo@bao.c"))
