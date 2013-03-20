<img src="https://raw.github.com/mcohen01/amazonica/master/claws.png" style="float:left;" />
# `Amazonica`

A comprehensive Clojure client for the entire [Amazon AWS api] [1].   

## Installation  

Leiningen coordinates:
```clj
[amazonica "0.1.0"]
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
  <version>0.1.0</version>
</dependency>
```

## Supported Services
* Autoscaling
* CloudFormation
* CloudFront
* CloudSearch
* CloudWatch
* DataPipeline
* DirectConnect
* DynamoDB
* EC2
* ElastiCache
* ElasticBeanstalk
* ElasticLoadBalancing
* ElasticMapReduce
* Glacier
* IdentityManagement
* OpsWorks
* RDS
* Redshift
* Route53
* S3
* SimpleDB
* SimpleEmail
* SNS
* SQS
* StorageGateway

    
## Features
* Follows idiomatic Clojure naming conventions
* Clojure data in, Clojure data out
* JodaTime support
* Support for multiple AWS credentials
    
## Documentation   
[Minimum Viable Snippet] [9]:  
```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.ec2]))

  (defcredential "aws-access-key" "aws-secret-key" "us-west-1")
  
  (describe-instances)

  (create-snapshot :volume-id   "vol-8a4857fa"
                   :description "my_new_snapshot")
```  

Amazonica reflectively delegates to the Java client library, as such it supports the complete set of remote service calls implemented by each of the service-specific AWS client classes (e.g. AmazonEC2Client, AmazonS3Client, etc.), the documentation for which can be found  in the [AWS Javadocs] [2].   
Reflection is used to create idiomatically named Clojure Vars in the library namespaces corresponding to the AWS service. camelCase Java methods become lower-case, hyphenated Clojure functions. So for example, if you want to create a snapshot of a running EC2 instance, you'd simply
```clj
(use 'amazonica.core 'amazonica.aws.ec2)
(create-snapshot :volume-id "vol-8a4857fa"
                 :description "my_new_snapshot")
```
which delegates to the [createSnapshot()] [3] method of AmazonEC2Client. If the Java method on the Amazon*Client takes a parameter, such as [CreateSnapshotRequest] [4] in this case, the bean properties exposed via mutators of the form set* can be supplied as key-value pairs passed as arguments to the Clojure function.   

All of the AWS Java apis (except S3) follow this pattern, either having a single implementation which takes an AWS Java bean as its only argument, or being overloaded and having a no-arg implementation. The corresponding Clojure function will either require key-value pairs as arguments, or be variadic and allow a no-arg invocation.   

For example, AmazonEC2Client's [describeImages()] [7] method is overloaded, and can be invoked either with no args, or with a [DescribeImagesRequest] [8]. So the Clojure invocation would look like
```clj
(describe-images)
```
or
```clj
(describe-images :owners ["self"]
                 :image-ids ["ami-f00f9699" "ami-e0d30c89"])
```   
Note that `java.util.Collections` are supported as arguments (as well as being converted to Clojure persistent data structures in the case of return values). Typically when service calls take collections as parameter arguments, as in the case above, the values in the collections are often instances of the Java wrapper classes. Smart conversions are attempted based on the argument types of the underlying Java method signature, and are generally transparent to the user, such as Clojure's preferred longs being converted to ints where required. `java.util.Date` and Joda Time `org.joda.time.base.AbstractInstant` are supported as well. In cases where collection arguments contain instances of AWS "model" classes, Clojure maps will be converted to the appropriate AWS Java bean instance. So for example, [describeAvailabilityZones()] [5] can take a [DescribeAvailabilityZonesRequest] [6] which itself has a `filters` property which is a java.util.List of `com.amazonaws.services.ec2.model.Filter`s. Passing the filters argument would look like:
```clj
(describe-availability-zones 
  :zone-names ["us-east-1a" "us-east-1b"]
  :filters [
    {:name "environment"
     :values ["dev" "qa" "staging"]}])
```

### Authentication
You'll note that none of the functions take an explicit credentials (key pair) argument. Typical usage would see users calling `(defcredential)` before invoking any service functions and passing in their AWS key pair and an optional endpoint:  
```clj
(defcredential "aws-access-key" "aws-secret-key" "us-west-1")
```  
All subsequent API calls will use the specified credential. If you need to execute a service call with alternate credentials, or against a different region than the one passed to `(defcredential)`, you can wrap these ad-hoc calls in the `(with-credential) macro, which takes a vector of key pair credentials and an optional endpoint, like so:  
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


## Example Usage

###EC2
```clj
(ns com.example
  (:use [amazonica.core]
        [amazonica.aws.ec2]))
        
(describe-images :owners ["self"])
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
; ....
```
```clojure
(describe-instances)
```
```clojure
(create-image 
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
```
&nbsp;  
###S3

&nbsp;  
###DynamoDB

&nbsp;  
###Redshift

&nbsp;  
###ElasticMapReduce

&nbsp;  
## License

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