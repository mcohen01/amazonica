(ns amazonica.test.core
  (:import amazonica.TreeHash
           org.joda.time.DateTime
           java.io.BufferedInputStream
           java.io.File
           java.io.FileInputStream
           java.text.SimpleDateFormat
           java.util.Date
           java.util.UUID)
  (:require [clojure.string :as str])
  (:use [clojure.test]
        [clojure.pprint]
        [clojure.java.shell]
        [amazonica.core]
        [amazonica.aws.autoscaling      :exclude (delete-tags
                                                  describe-tags
                                                  get-service-abbreviation)]
        [amazonica.aws.elasticache      :exclude (describe-events )]
        [amazonica.aws.elasticbeanstalk :exclude (describe-events)]
        [amazonica.aws.rds              :exclude (describe-engine-default-parameters)]
        [amazonica.aws.redshift         :exclude (describe-events)]
        [amazonica.aws.simpledb         :exclude (create-domain
                                                  delete-domain)]
        [amazonica.aws.sns              :exclude (add-permission
                                                  remove-permission)]
        [amazonica.aws.storagegateway   :exclude (create-snapshot
                                                  delete-volume)]
        [amazonica.aws.glacier          :exclude (abort-multipart-upload
                                                  complete-multipart-upload
                                                  initiate-multipart-upload
                                                  list-multipart-uploads
                                                  list-parts)]
        [amazonica.aws.opsworks         :exclude (create-stack
                                                  delete-stack
                                                  describe-instances
                                                  describe-stacks
                                                  describe-volumes
                                                  update-stack)]
        [amazonica.aws
          cloudformation
          cloudfront
          cloudsearch
          cloudwatch
          datapipeline
          directconnect
          dynamodb
          ec2
          elasticloadbalancing
          elasticmapreduce
          identitymanagement
          route53
          s3
          simpleemail
          sqs]))

; config file contains space-separated AWS credential key pair
; and optional third param of AWS endpoint (e.g. for different
; region than the default US_East)
(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))

(deftest opsworks []
  (create-stack
    cred
    :name "my-stack"
    :region "us-east-1"
    :default-os "Ubuntu 12.04 LTS"
    :service-role-arn "arn:aws:iam::676820690883:role/aws-opsworks-service-role")

  (create-layer
    cred
    :name "webapp-layer"
    :stack-id "dafa328e-c529-41af-89d3-12840a31abad"
    :enable-auto-healing true
    :auto-assign-elastic-ips true
    :volume-configurations [
      {:mount-point "/data"
       :number-of-disks 1
       :size 50}])

  (describe-stacks cred :stack-ids ["dafa328e-c529-41af-89d3-12840a31abad"])
)

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

  (delete-bucket-cross-origin-configuration cred bucket1)
  (delete-bucket-lifecycle-configuration cred bucket1)
  (delete-bucket-policy cred bucket1)
  (delete-bucket-tagging-configuration cred bucket1)
  (delete-bucket-website-configuration cred bucket1)

  
  (get-s3account-owner cred)

  (let [b (:s3bucket (create-storage-location cred))
        _ (println "created bucket" b)]
    (delete-bucket cred b))

  (list-buckets cred)
  
  (does-bucket-exist cred bucket1)

  (generate-presigned-url cred bucket1 "jenny" date)

  (get-bucket-acl cred bucket1)

  (get-bucket-location cred bucket1)

  (get-bucket-policy cred bucket1)

  (get-bucket-website-configuration cred bucket1)

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

  (try
    (get-object cred
                :bucket-name bucket1
                :key "jenny"
                download-file)
    (catch Exception e
      (.printStackTrace e)))

  (copy-object cred bucket1 "jenny" bucket2 "jenny")
    

  (generate-presigned-url cred bucket1 "jenny" date)
  (generate-presigned-url cred bucket2 "jenny" date)

  (delete-object cred bucket1 "jenny")
  (delete-object cred bucket2 "jenny")

  (delete-bucket cred bucket1)
  (delete-bucket cred bucket2)

  (if (.exists download-file)
    (.delete download-file))

)


(deftest redshift []

  ; config file contains space-separated AWS credential key pair
  ; and optional third param of AWS endpoint (e.g. for different
  ; region than the default US_East)
  (apply defcredential 
    (seq (.split (slurp "aws.config") " ")))


  (println (describe-cluster-versions cred))
  (println (describe-clusters))


  (try
    (create-cluster-subnet-group cred
                                 :cluster-subnet-group-name "my subnet"
                                 :description "some desc"
                                 :subnet-ids ["1" "2" "3" "4"])
    (throw (Exception. "create-cluster-subnet-group did not throw exception"))
    (catch Exception e
      (is (.contains 
            (:message (ex->map e)) 
            "Some input subnets in :[1, 2, 3, 4] are invalid."))))
    
 (amazonica.aws.redshift/describe-events :source-type "Cluster")

  (try
    (modify-cluster-parameter-group :parameter-group-name "myparamgroup"
                                   :parameters [
                                    {:source          "user"
                                     :parameter-name  "my_new_param"
                                     :parameter-value "some value"
                                     :data-type       "String"
                                     :description     "some generic param"}

                                    {:source          "user"
                                     :parameter-name  "my_new_param-2"
                                     :parameter-value 42
                                     :data-type       "Number"
                                     :description     "some integer param"}])
    (throw (Exception. "modify-cluster-parameter-group did not throw exception"))
    (catch Exception e
      (is (.contains
            (:message (ex->map e))
            "Could not find parameter with name: my_new_param"))))

)


(deftest dynamodb []  

  (create-table cred 
                :table-name "TestTable"
                :key-schema {
                  :hash-key-element {
                    :attribute-name "id"
                    :attribute-type "S"
                  }
                }
                :provisioned-throughput {
                  :read-capacity-units 1
                  :write-capacity-units 1
                })

  (create-table cred
                :table-name "TestTable2"
                :key-schema {
                  :hash-key-element {
                    :attribute-name "id"
                    :attribute-type "S"
                  }
                  :range-key-element {
                    :attribute-name "range"
                    :attribute-type "S"
                  }
                }
                :provisioned-throughput {
                  :read-capacity-units 1
                  :write-capacity-units 1
                })

  (create-table cred
                :table-name "TestTable3"
                :key-schema {
                  :hash-key-element {
                    :attribute-name "id"
                    :attribute-type "S"
                  }
                }
                :provisioned-throughput [1 1])

  ; wait for the tables to be created
  (doseq [table (:table-names (list-tables cred))]
    (loop [status (get-in (describe-table cred :table-name table)
                          [:table :table-status])]
      (if-not (= "ACTIVE" status)
        (do 
          (println "waiting for status" status "to be active")
          (Thread/sleep 1000)
          (recur (get-in (describe-table cred :table-name table)
                          [:table :table-status]))))))
 

  (set-root-unwrapping! true)

  (is (= "id" 
         (get-in 
           (describe-table cred :table-name "TestTable")
           [:key-schema :hash-key-element :attribute-name])))

  (set-root-unwrapping! false)

  (is (= "id" 
         (get-in 
           (describe-table cred :table-name "TestTable")
           [:table :key-schema :hash-key-element :attribute-name])))
  
  (list-tables cred)
  (list-tables cred :limit 1)

  (dotimes [x 10] 
    (let [m {:id (str "1234" x) :text "joey t"}]
      (put-item cred 
                :table-name "TestTable" 
                :item m)))

  (try
    (put-item cred
            :table-name "TestTable"
            :item {
              :id "foo" 
              :text "barbaz"
            })
    (catch Exception e
      (.printStackTrace e)))

  (put-item cred
            :table-name "TestTable2"
            :item {
              :id { :s "foo" }
              :range { :s "foo" } 
              :text { :s "zonica" }
            })
  (try
    (get-item cred
              :table-name "TestTableXXX"
              :key "foo")
  (catch Exception e
    (let [error-map (ex->map e)]
      (is (contains? error-map :error-code))
      (is (contains? error-map :status-code))
      (is (contains? error-map :service-name))
      (is (contains? error-map :message)))))

  (query cred
         :table-name "TestTable2"
         :limit 1
         :hash-key-value "mofo"
         :range-key-condition {
           :attribute-value-list ["f"]
           :comparison-operator "BEGINS_WITH"
          })

  (clojure.pprint/pprint
    (scan cred :table-name "TestTable"))

  (set-root-unwrapping! false)

  (clojure.pprint/pprint (batch-get-item cred :request-items {
     "TestTable" { :keys [
                     {:hash-key-element {:s "foo"}}
                     {:hash-key-element {:s "1234"}}
                   ]
                   :consistent-read true
                   :attributes-to-get ["id" "text"]}}))

  
  #_(try
    (batch-write-item cred :request-items {
    "TestTable" [
      {:put-request {
        :item {
          :id "1234"
          :text "mofo"}}}
      {:put-request {
        :item {
          :id "foo"
          :text "barbarbanks"}}}]})
    (catch Exception e
      (println (.printStackTrace e))))

  (clojure.pprint/pprint 
    (describe-table cred :table-name "TestTable"))

  (delete-table cred :table-name "TestTable")
  (delete-table cred :table-name "TestTable2")
  (delete-table cred :table-name "TestTable3")

)


(deftest ec2 []

  ; config file contains space-separated AWS credential key pair
  ; and optional third param of AWS endpoint (e.g. for different
  ; region than the default US_East)
  (apply defcredential 
    (seq (.split (slurp "aws.config") " ")))

  (clojure.pprint/pprint
    (list-available-solution-stacks cred))

  (clojure.pprint/pprint
    (describe-availability-zones cred))

  (clojure.pprint/pprint
    (describe-dhcp-options))

  #_(clojure.pprint/pprint
    (describe-images cred :owners ["self"]))

  (clojure.pprint/pprint
    (describe-instances))

  (let [image-id (create-image 
                  :name "my_test_image"
                  :instance-id "i-1b9a9f71"
                  :description "test image - safe to delete"
                  :block-device-mappings [
                    {:device-name  "/dev/sda1"
                     :virtual-name "myvirtual"
                     :ebs {
                       :volume-size 8
                       :volume-type "standard"
                       :delete-on-termination true}}])]
    (deregister-image :image-id (:image-id image-id)))
    ;(deregister-image :image-id "ami-f00f9699")
)


(deftest cloudwatch []

  (clojure.pprint/pprint
    (list-metrics :metric-name "ThrottledRequests"
                  :namespace "AWS/DynamoDB"))

  (set-date-format! "MM-dd-yyyy")

  (clojure.pprint/pprint
    (let [date-string (.. (SimpleDateFormat. "MM-dd-yyyy")
                          (format (Date.)))]
      (get-metric-statistics :metric-name "ThrottledRequests"
                             :namespace "AWS/DynamoDB"
                             :start-time (.minusDays (DateTime.) 1)
                             :end-time date-string
                             :period 60
                             :statistics ["Sum" "Maximum" "Minimum" 
                                          "SampleCount" "Average"])))

  (clojure.pprint/pprint
    (describe-alarms :max-records 100))

  (clojure.pprint/pprint
    (describe-alarm-history :max-records 100))
)

(deftest datapipeline []
  
  (let [pid (:pipeline-id
              (create-pipeline
                cred
                :name "my-pipeline"
                :unique-id "mp"))]
    (clojure.pprint/pprint
      (describe-pipelines cred :pipeline-ids [pid]))
    (delete-pipeline cred :pipeline-id pid))

  (list-pipelines cred)

)

(deftest glacier []

  (def upload-file (java.io.File. "upload.txt"))
  
  (create-vault cred :vault-name "my-vault")
  
  (clojure.pprint/pprint
    (describe-vault cred :vault-name "my-vault"))
  
  (clojure.pprint/pprint
    (list-vaults cred :limit 10))

  (upload-archive
    cred
    :vault-name "my-vault"
    :body "upload.txt")
  
  (delete-archive
    cred
    :account-id "-"
    :vault-name "my-vault"
    :archive-id "pgy30P2FTNu_d7buSVrGawDsfKczlrCG7Hy6MQg53ibeIGXNFZjElYMYFm90mHEUgEbqjwHqPLVko24HWy7DU9roCnZ1djEmT-1REvnHKHGPgkuzVlMIYk3bn3XhqxLJ2qS22EYgzg", :checksum "83a05fd1ce759e401b44fff8f34d40e17236bbdd24d771ec2ca4886b875430f9", :location "/676820690883/vaults/my-vault/archives/pgy30P2FTNu_d7buSVrGawDsfKczlrCG7Hy6MQg53ibeIGXNFZjElYMYFm90mHEUgEbqjwHqPLVko24HWy7DU9roCnZ1djEmT-1REvnHKHGPgkuzVlMIYk3bn3XhqxLJ2qS22EYgzg")    

  (.delete upload-file)

)

(deftest route53 []

  (create-health-check
    cred
    :health-check-config
      {:port 80,
       :type "HTTP",
       :ipaddress "127.0.0.1",
       :fully-qualified-domain-name "example.com"})

  (get-health-check
    cred
    :health-check-id "ce6a4aeb-acf1-4923-a116-cd9ae2c30ee3")

  (create-hosted-zone
    cred
    :name "example.com.")

  (get-hosted-zone cred :id "Z3TKY0VR5CH45U")
  
  (list-hosted-zones cred)

  (list-health-checks cred)

  (list-resource-record-sets
    cred
    :hosted-zone-id "ZN8D0HXQLVRRL")

  (delete-health-check
    cred
    :health-check-id "99999999-1234-4923-a116-cd9ae2c30ee3")

  (delete-hosted-zone cred :id "my-bogus-hosted-zone")

)

(deftest sns []

  (create-topic cred :name "my-topic")
  
  (list-topics cred)

  (subscribe
    cred
    :protocol "email"
    :topic-arn "arn:aws:sns:us-east-1:676820690883:my-topic"
    :endpoint "mcohen01@gmail.com")

  (clojure.pprint/pprint
    (list-subscriptions cred))

  (publish
    cred
    :topic-arn "arn:aws:sns:us-east-1:676820690883:my-topic"
    :subject "test"
    :message (str "Todays is " (java.util.Date.)))

  (unsubscribe
    cred
    :subscription-arn
    "arn:aws:sns:us-east-1:676820690883:my-topic:33fb2721-b639-419f-9cc3-b4adec0f4eda")

)

(deftest sqs []
  
  (create-queue
    cred
    :queue-name "my-queue"
    :attributes
      {:VisibilityTimeout 30 ; sec
       :MaximumMessageSize 65536 ; bytes
       :MessageRetentionPeriod 1209600 ; sec
       :ReceiveMessageWaitTimeSeconds 10}) ; sec

  (def q (get (:queue-urls (list-queues cred)) 0))

  (send-message
    cred
    :queue-url q
    :delay-seconds 0
    :message-body (str "test" (java.util.Date.)))
  
  (clojure.pprint/pprint
  (receive-message
    cred
    :queue-url q
    :wait-time-seconds 6
    :max-number-of-messages 10
    :delete true
    :attribute-names ["SenderId" "ApproximateFirstReceiveTimestamp" "ApproximateReceiveCount" "SentTimestamp"]))

  (delete-queue
    cred
    :queue-url q)
)