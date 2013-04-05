![AWS logo] (claws.png)
# `Amazonica`

A comprehensive Clojure client for the entire [Amazon AWS api] [1].   

## Installation  

Leiningen coordinates:
```clj
[amazonica "0.1.4"]
```

For Maven users:

add the following repository definition to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```
and the following dependency:

``` xml
<dependency>
  <groupId>amazonica</groupId>
  <artifactId>amazonica</artifactId>
  <version>0.1.4</version>
</dependency>
```

## Supported Services
* [Autoscaling] (#autoscaling)
* [CloudFormation] (#cloudformation)
* [CloudFront] (#cloudfront)
* [CloudSearch] (#cloudsearch)
* [CloudWatch] (#cloudwatch)
* [DataPipeline] (#datapipeline)
* DirectConnect
* [DynamoDB] (#dynamodb)
* [EC2] (#ec2)
* ElastiCache
* ElasticBeanstalk
* ElasticLoadBalancing
* ElasticMapReduce
* [Glacier] (#glacier)
* IdentityManagement
* OpsWorks
* RDS
* [Redshift] (#redshift)
* Route53
* [S3] (#s3)
* SimpleDB
* SimpleEmail
* [SNS] (#sns)
* SQS
* StorageGateway

    
## Documentation   
[Minimum Viable Snippet] [9]:  
```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.ec2]))

  (def cred {:access-key "aws-access-key"
             :secret-key "aws-secret-key"})
  
  (describe-instances cred)

  (create-snapshot cred
                   :volume-id   "vol-8a4857fa"
                   :description "my_new_snapshot")
```  

Amazonica reflectively delegates to the Java client library, as such it supports the complete set of remote service calls implemented by each of the service-specific AWS client classes (e.g. AmazonEC2Client, AmazonS3Client, etc.), the documentation for which can be found  in the [AWS Javadocs] [2].  

Reflection is used to create idiomatically named Clojure Vars in the library namespaces corresponding to the AWS service. camelCase Java methods become lower-case, hyphenated Clojure functions. So for example, if you want to create a snapshot of a running EC2 instance, you'd simply
```clj
(create-snapshot cred
                 :volume-id "vol-8a4857fa"
                 :description "my_new_snapshot")
```
which delegates to the [createSnapshot()] [3] method of AmazonEC2Client. If the Java method on the Amazon*Client takes a parameter, such as [CreateSnapshotRequest] [4] in this case, the bean properties exposed via mutators of the form set* can be supplied as key-value pairs passed as arguments to the Clojure function.   

All of the AWS Java apis (except S3) follow this pattern, either having a single implementation method which takes an AWS Java bean as its only argument, or being overloaded and having a no-arg implementation. The corresponding Clojure function will either require key-value pairs as arguments, or be variadic and allow a no-arg invocation.   

For example, AmazonEC2Client's [describeImages()] [7] method is overloaded, and can be invoked either with no args, or with a [DescribeImagesRequest] [8]. So the Clojure invocation would look like
```clj
(describe-images cred)
```
or
```clj
(describe-images cred
                 :owners ["self"]
                 :image-ids ["ami-f00f9699" "ami-e0d30c89"])
```   

### Conversion of Returned Types  

`java.util.Collections` are converted to the corresponding Clojure collection type. `java.util.Maps` are converted to `clojure.lang.IPersistenMaps`, `java.util.Lists` are converted to `clojure.lang.IPersistenVectors`, etc.  

`java.util.Dates` are automatically converted to Joda Time `DateTime` instances.   

Amazon AWS object types are returned as Clojure maps, with conversion taking place recursively, so, "Clojure data all the way down."  

For example, a call to 
```clj
(describe-instances cred)
```
invokes a Java method on AmazonEC2Client which returns a `com.amazonaws.services.ec2.model.DescribeInstancesResult`. However, this is recursively converted to Clojure data, yielding a map of `Reservations`, like so:
```clj
{:owner-id "676820690883",
   :group-names ["cx"],
   :groups [{:group-name "cx", :group-id "sg-38f45150"}],
   :instances
   [{:instance-type "m1.large",
     :kernel-id "aki-825ea7eb",
     :hypervisor "xen",
     :state {:name "running", :code 16},
     :ebs-optimized false,
     :public-dns-name "ec2-154-73-176-213.compute-1.amazonaws.com",
     :root-device-name "/dev/sda1",
     :virtualization-type "paravirtual",
     :root-device-type "ebs",
     :block-device-mappings
     [{:device-name "/dev/sda1",
       :ebs
       {:status "attached",
        :volume-id "vol-b0e519c3",
        :attach-time #<DateTime 2013-03-21T22:00:56.000-07:00>,
        :delete-on-termination true}}],
     :network-interfaces [],
     :public-ip-address "164.73.176.213",
     :placement
     {:availability-zone "us-east-1a",
      :group-name "",
      :tenancy "default"},
     :private-ip-address "10.116.187.19",
     :security-groups [{:group-name "cx", :group-id "sg-38f45150"}],
     :state-transition-reason "",
     :private-dns-name "ip-10-116-187-19.ec2.internal",
     :instance-id "i-cefbe7a2",
     :key-name "cxci",
     :architecture "x86_64",
     :client-token "",
     :image-id "ami-baba68d3",
     :ami-launch-index 0,
     :monitoring {:state "disabled"},
     :product-codes [],
     :launch-time #<DateTime 2013-03-21T22:00:52.000-07:00>,
     :tags [{:value "CXCI_nightly", :key "Name"}]}],
   :reservation-id "r-8a23d6f7"}
```
If you look at the `Reservation` [Javadoc] [10] you'll see that `getGroups()` returns a `java.util.List` of `GroupIdentifiers`, which is converted to a vector of maps containing keys `:group-name` and `:group-id`, under the `:groups` key. Ditto for :block-device-mappings and :tags, and so and so on...

Similar in concept to JSON unwrapping in Jackson, Amazonica supports root unwrapping of the returned data. So calling 
```clj
; dynamodb
(list-tables cred)
```
by default would return 
```clj
{:table-names ["TableOne" "TableTwo" "TableThree"]}
```
However, if you call 
```clj
(set-root-unwrapping! true)
```
then single keyed top level maps will be "unwrapped" like so:
```clj
(list-tables cred)
=> ["TableOne" "TableTwo" "TableThree"]
```



The returned data can be "round tripped" as well. So the returned Clojure data structures can be supplied as arguments to function calls which delegate to Java methods taking the same object type as an argument. See the section below for more on this.  

### Argument Coercion   

Coercion of any types that are part of the java.lang wrapper classes happens transparently. So for example, Clojure's preferred longs are automatically converted to ints where required. 

Clojure data structures automatically participate in the Java Collections abstractions, and so no explicit coercion is necessary. Typically when service calls take collections as parameter arguments, as in the case above, the values in the collections are most often instances of the Java wrapper classes.  

When complex objects consisting of types outside of those in the `java.lang` package are required as argument parameters, smart conversions are attempted based on the argument types of the underlying Java method signature. Methods requiring a `java.util.Date` argument can take Joda Time `org.joda.time.base.AbstractInstants`, longs, or Strings (default pattern is "yyyy-MM-dd"), with conversion happening automatically. 
```clj 
(set-date-format! "MM-dd-yyyy")
``` 
can be used to set the pattern supplied to the underlying `java.text.SimpleDateFormat`.  

In cases where collection arguments contain instances of AWS "model" classes, Clojure maps will be converted to the appropriate AWS Java bean instance. So for example, [describeAvailabilityZones()] [5] can take a [DescribeAvailabilityZonesRequest] [6] which itself has a `filters` property, which is a `java.util.List` of `com.amazonaws.services.ec2.model.Filters`. Passing the filters argument would look like:
```clj
(describe-availability-zones
  cred 
  :filters [
    {:name "environment"
     :values ["dev" "qa" "staging"]}])
```
and return the following Clojure collection:
```clj
{:availability-zones
 [{:state "available",
   :region-name "us-east-1",
   :zone-name "us-east-1a",
   :messages []}
  {:state "available",
   :region-name "us-east-1",
   :zone-name "us-east-1b",
   :messages []}
  {:state "available",
   :region-name "us-east-1",
   :zone-name "us-east-1c",
   :messages []}
  {:state "available",
   :region-name "us-east-1",
   :zone-name "us-east-1d",
   :messages []}
  {:state "available",
   :region-name "us-east-1",
   :zone-name "us-east-1e",
   :messages []}]}
```  


### Extension points  
Clojure apis built specifically to wrap a Java client, such as this one, often provide "conveniences" for the user of the api, to remove boilerplate. In Amazonica this is accomplished via the IMarshall protocol, which defines the contract for converting the returned Java result from the AWS service call to Clojure data, and the  
```clj 
(amazonica.core/register-coercions) 
``` 
function, which takes a map of class/function pairs defining how a value should be coerced to a specific AWS Java bean. You can find a good example of this in the `amazonica.aws.dynamodb` namespace. Consider the following DynamoDB service call:  
```clj
(get-item cred
          :table-name "MyTable"
          :key "foo")
```
The [GetItemRequest] [11] takes a `com.amazonaws.services.dynamodb.model.Key` which is composed of a hash key of type `com.amazonaws.services.dynamodb.model.AttributeValue` and optional range key also of type `AttributeValue`. Without the coercions registered for `Key` and `AttributeValue` in `amazonica.aws.dynamodb` we would need to write:  
```clj
(get-item cred
          :table-name "TestTable"
          :key {
            :hash-key-element {
              :s "foo"}})
```  
Note that either form will work. This allows contributors to the library to incrementally evolve the api independently from the core of the library, as well as maintain backward compatibility of existing code written against prior versions of the library which didn't contain the conveniences. 


### Authentication
All of the functions take as their first argument an explicit map of credentials, with keys :access-key and :secret-key, and optional :endpoint. (Default endpoint is "us-east-1") 

```clj
(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"
           :endpoint   "us-west-1"})

(describe-instances cred)
```  

As a convenience, users may call `(defcredential)` before invoking any service functions and passing in their AWS key pair and an optional endpoint:  
```clj
(defcredential "aws-access-key" "aws-secret-key" "us-west-1")
```  
All subsequent API calls will use the specified credential. If you need to execute a service call with alternate credentials, or against a different region than the one passed to `(defcredential)`, you can wrap these ad-hoc calls in the `(with-credential)` macro, which takes a vector of key pair credentials and an optional endpoint, like so:  
```clj
(defcredential "account-1-aws-access-key" "aws-secret-key" "us-west-1")

(describe-instances)
; returns instances visible to account-1

(with-credential ["account-2-aws-access-key" "secret" "us-east-1"]
  (describe-instances))
; returns EC2 instances visible to account-2 running in US-East region

(describe-images :owners ["self"])
; returns images belonging to account-1
```  

### Exception Handling  
All functions throw `com.amazonaws.AmazonServiceExceptions`. If you wish to catch exceptions you can convert the AWS object to a Clojure map like so:
```clj
(try
  (create-snapshot :volume-id "vol-ahsg23h"
                   :description "daily backup")
  (catch Exception e
    (log (ex->map e))))

; {:error-code "InvalidParameterValue",
;  :error-type "Unknown",
;  :status-code 400,
;  :request-id "9ba69e16-ed63-41d4-ac02-1f6032cb64de",
;  :service-name "AmazonEC2",
;  :message
;  "Value (vol-ahsg23h) for parameter volumeId is invalid. Expected: 'vol-...'.",
;  :stack-trace "Status Code: 400, AWS Service: AmazonEC2, AWS Request ID: a5b0340a-8f37-4122-941c-ed8d5472b11d, AWS Error Code: InvalidParameterValue, AWS Error Message: Value (vol-ahsg23h) for parameter volumeId is invalid. Expected: 'vol-...'. 
;  at com.amazonaws.http.AmazonHttpClient.handleErrorResponse(AmazonHttpClient.java:644)
;   at com.amazonaws.http.AmazonHttpClient.executeHelper(AmazonHttpClient.java:338)
;   at com.amazonaws.http.AmazonHttpClient.execute(AmazonHttpClient.java:190)
;   at com.amazonaws.services.ec2.AmazonEC2Client.invoke(AmazonEC2Client.java:6199)
;   at com.amazonaws.services.ec2.AmazonEC2Client.createSnapshot(AmazonEC2Client.java:1531)
;   .....
```
### Performance  
Amazonica uses reflection extensively, to generate the public Vars, to set the bean properties passed as arguments to those functions, and to invoke the actual service method calls on the underlying AWS Client class. As such, one may wonder if such pervasive use of reflection will result in unacceptable performance. In general, this shouldn't be an issue, as the cost of reflection should be relatively minimal compared to the latency incurred by making a remote call across the network. Furthermore, typical AWS usage is not going to be terribly concerned with performance, except with specific services such as DynamoDB, RDS, SimpleDB, or SQS. But we have done some basic benchmarking against the excellent DynamoDB [rotary] [13] library, which uses no explicit reflection. Results are shown below. Benchmarking code is available at [https://github.com/mcohen01/amazonica-benchmark] [12]  

![Benchmark results](https://raw.github.com/mcohen01/amazonica-benchmark/master/reflection.png)



## Examples

###Autoscaling  

```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.autoscaling]))

(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"})

(create-launch-configuration cred
                             :launch-configuration-name "aws_launch_cfg"
                             :block-device-mappings [
                              {:device-name "/dev/sda1"
                               :virtual-name "vol-b0e519c3"
                               :ebs
                                {:snapshot-id "snap-36295e51"
                                 :volume-size 32}}]
                             :ebs-optimized true
                             :image-id "ami-6fde0d06"
                             :instance-type "m1.large"
                             :spot-price ".10")

(create-auto-scaling-group cred
                           :auto-scaling-group-name "aws_autoscale_grp"
                           :availability-zones ["us-east-1a" "us-east-1b"]
                           :desired-capacity 3
                           :health-check-grace-period 300
                           :health-check-type "EC2"
                           :launch-configuration-name "aws_launch_cfg"
                           :min-size 3
                           :max-size 3)

(describe-auto-scaling-instances cred)  

```  

###CloudFormation  
```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.cloudformation]))

(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"})
      
(create-stack cred
              :stack-name "my-stack"
              :template-url "abcd1234.s3.amazonaws.com")

(describe-stack-resources cred)  

```

###CloudFront  
```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.cloudfront]))

(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"})

(create-distribution 
  cred
  :distribution-config {
  :enabled true
  :default-root-object "index.html"
  :origins
   {:quantity 0
    :items []}
  :logging
   {:enabled false
    :include-cookies false
    :bucket "abcd1234.s3.amazonaws.com"
    :prefix "cflog_"}                     
  :caller-reference 12345
  :aliases
   {:items ["m.example.com" "www.example.com"]
    :quantity 2}
  :cache-behaviors
   {:quantity 0 
    :items []}
  :comment "example"
  :default-cache-behavior
   {:target-origin-id "MyOrigin"
    :forwarded-values
      {:query-string false 
       :cookies 
         {:forward "none"}}}
   :trusted-signers
     {:enabled false
      :quantity 0}
   :viewer-protocol-policy "allow-all"
   :min-ttl 3600}
  :price-class "PriceClass_All"})

(list-distributions cred :max-items 10)
; {:distribution-list
;  {:is-truncated false,
;   :quantity 3,
;   :marker "",
;   :items
;   [{:status "Deployed",
;     :enabled true,
;     :comment "wordfront",
;     :origins
;     {:quantity 1,
;      :items
;      [{:s3origin-config {:origin-access-identity ""},
;        :id "MyOrigin",
;        :domain-name "myblog.s3.amazonaws.com"}]},
;     :last-modified-time #<DateTime 2010-10-13T13:56:46.556-07:00>,
;     :cache-behaviors {:quantity 0, :items []},
;     :domain-name "dhpz2lx23abcd.cloudfront.net",
;     :default-cache-behavior
;     {:target-origin-id "MyOrigin",
;      :forwarded-values
;      {:query-string false, :cookies {:forward "none"}},
;      :trusted-signers {:quantity 0, :enabled false, :items []},
;      :viewer-protocol-policy "allow-all",
;      :min-ttl 3600},
;     :price-class "PriceClass_All",
;     :id "E5GB5B26FIF5A",
;     :aliases {:quantity 1, :items ["blogcdn.example.com"]}}]}}  

```

###CloudSearch  
```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.cloudsearch]))

(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"})

(create-domain cred :domain-name "my-index")

(index-documents cred :domain-name "my-index")  

```

###CloudWatch  
```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.cloudwatch]))

(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"})

(put-metric-alarm cred
                  :alarm-name "my-alarm"
                  :actions-enabled true
                  :evaluation-periods 5
                  :period 60
                  :metric-name "CPU"
                  :threshold "50%")  

```

###DataPipeline  
```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.datapipeline]))

(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"})

(create-pipeline
  cred
  :name "my-pipeline"
  :unique-id "mp")

(put-pipeline-definition
  cred
  :pipeline-id "df-07746012XJFK4DK1D4QW"
  :pipeline-objects [
    {:name "my-pipeline-object"
     :id "my-pl-object-id"
     :fields [
       {:key "some-key"
        :string-value "foobar"}]}])  

(list-pipelines cred)

(delete-pipeline cred :pipeline-id pid)  

```

###DynamoDB  
```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.dynamodb]))

(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"})

(create-table cred
              :table-name "TestTable"
              :key-schema
                {:hash-key-element
                 {:attribute-name "id"
                  :attribute-type "S"}}              
              :provisioned-throughput
                {:read-capacity-units 1
                 :write-capacity-units 1})

(put-item cred
          :table-name "TestTable"
          :item
            {:id "foo" 
             :text "barbaz"})              

(get-item cred
          :table-name "TestTable"
          :key "foo")

(scan cred :table-name "TestTable")

(delete-table cred :table-name "TestTable")  

```

###EC2
```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.ec2]))

(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"})

(describe-images cred :owners ["self"])
; {:images
;  [{:kernel-id "aki-8e5ea7e7",
;    :hypervisor "xen",
;    :state "available",
;    :name "CentOS 6.2 (dev deploy)",
;    :root-device-name "/dev/sda1",
;    :virtualization-type "paravirtual",
;    :root-device-type "ebs",
;    :block-device-mappings
;    [{:device-name "/dev/sda",
;      :ebs
;      {:snapshot-id "snap-36295e51",
;       :volume-type "standard",
;       :delete-on-termination true,
;       :volume-size 6}}
;     {:device-name "/dev/sdf",
;      :ebs
;      {:snapshot-id "snap-32295e55",
;       :volume-type "standard",
;       :volume-size 5}}],
;    :image-location "676820690883/CentOS 6.2 (dev deploy)",
;    :image-type "machine",
;    :architecture "x86_64",
;    :image-id "ami-6fde0d06",
;    :owner-id "676820690883",
;    :product-codes [],
;    :description "Use this to spin up development instances",
;    :tags [{:value "CentOS 6.2", :key "Name"}]}  



(describe-instances cred)

; {:reservations
; [{:owner-id "676820690883",
;  :reservation-id "r-8a7463e1",
;  :instances
;  [{:instance-type "m1.small",
;    :kernel-id "aki-92ba58fb",
;    :hypervisor "xen",
;    :state {:code 16, :name "running"},
;    :ebs-optimized false,
;    :public-dns-name "ec2-184-73-212-155.compute-1.amazonaws.com",
;    :root-device-name "/dev/sda1",
;    :virtualization-type "paravirtual",
;    :root-device-type "ebs",
;    :block-device-mappings
;    [{:device-name "/dev/sda1",
;      :ebs
;      {:attach-time #<DateTime 2010-09-24T01:30:19.000-07:00>,
;       :delete-on-termination true,
;       :volume-id "vol-d0745eb9",
;       :status "attached"}}],
;    :network-interfaces [],
;    :public-ip-address "164.73.212.155",
;    :placement
;    {:tenancy "default",
;     :availability-zone "us-east-1a",
;     :group-name ""},
;    :private-ip-address "10.194.22.227",
;    :security-groups [{:group-name "web", :group-id "sg-2582484c"}],
;    :state-transition-reason "",
;    :ramdisk-id "ari-94ba58fd",
;    :private-dns-name "ip-10-194-22-227.ec2.internal",
;    :instance-id "i-1b9a9f71",
;    :key-name "bootstrap",
;    :architecture "i386",
;    :client-token "",
;    :image-id "ami-cb8d61a2",
;    :ami-launch-index 0,
;    :monitoring {:state "enabled"},
;    :product-codes [],
;    :launch-time #<DateTime 2010-10-07T06:04:41.000-07:00>,
;    :tags
;    [{:value "Flowtown Blog", :key "Name"}
;     {:value "blog/homepage", :key "purpose"}]}],
;  :group-names ["web"],
;  :groups [{:group-name "web", :group-id "sg-2582484c"}]}
;  ....  

(create-image
  cred
  :name "my_test_image"
  :instance-id "i-1b9a9f71"
  :description "test image - safe to delete"
  :block-device-mappings [
    {:device-name  "/dev/sda1"
     :virtual-name "myvirtual"
     :ebs {
       :volume-size 8
       :volume-type "standard"
       :delete-on-termination true}}])

(create-snapshot cred
                 :volume-id   "vol-8a4857fa"
                 :description "my_new_snapshot")  

```


###Glacier  

```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.glacier]))

(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"})

(create-vault cred :vault-name "my-vault")
  
(describe-vault cred :vault-name "my-vault")
  
(list-vaults cred :limit 10)

(upload-archive
  cred
  :vault-name "my-vault"
  :body "upload.txt")
  
(delete-archive
  cred
  :account-id "-"
  :vault-name "my-vault"
  :archive-id "pgy30P2FTNu_d7buSVrGawDsfKczlrCG7Hy6MQg53ibeIGXNFZjElYMYFm90mHEUgEbqjwHqPLVko24HWy7DU9roCnZ1djEmT-1REvnHKHGPgkuzVlMIYk3bn3XhqxLJ2qS22EYgzg", :checksum "83a05fd1ce759e401b44fff8f34d40e17236bbdd24d771ec2ca4886b875430f9", :location "/676820690883/vaults/my-vault/archives/pgy30P2FTNu_d7buSVrGawDsfKczlrCG7Hy6MQg53ibeIGXNFZjElYMYFm90mHEUgEbqjwHqPLVko24HWy7DU9roCnZ1djEmT-1REvnHKHGPgkuzVlMIYk3bn3XhqxLJ2qS22EYgzg")
  
(delete-vault cred :vault-name "my-vault")  

 ```  


###Redshift  
```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.redshift]))

(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"})

(create-cluster cred
                :availability-zone "us-east-1a"
                :cluster-type "multi-node"
                :db-name "dw"
                :master-username "scott"
                :master-user-password "tiger"
                :number-of-nodes 3)  

```

###S3  
```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.s3]))

(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"})

(create-bucket cred "two-peas")

(put-object cred 
            :bucket-name "two-peas"
            :key "foo"
            :file upload-file)

(copy-object
  cred bucket1 "key-1" bucket2 "key-2")            

(generate-presigned-url
  cred bucket1 "key-1" (-> 6 hours from-now))  

```

###SNS  
```clj

(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.sns]))

(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"})


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

```

### License

Copyright (C) 2013 Michael Cohen

Distributed under the Eclipse Public License, the same as Clojure.

[1]: http://aws.amazon.com/documentation/
[2]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/index.html
[3]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/AmazonEC2Client.html#createSnapshot(com.amazonaws.services.ec2.model.CreateSnapshotRequest)
[4]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/model/CreateSnapshotRequest.html
[5]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/AmazonEC2Client.html#describeAvailabilityZones(com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest)
[6]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/model/DescribeAvailabilityZonesRequest.html
[7]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/AmazonEC2Client.html#describeImages()
[8]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/model/DescribeImagesRequest.html
[9]: http://blog.fogus.me/2012/08/23/minimum-viable-snippet/
[10]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/model/Reservation.html
[11]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodb/model/GetItemRequest.html
[12]:https://github.com/mcohen01/amazonica-benchmark
[13]:https://github.com/weavejester/rotary