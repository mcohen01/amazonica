# Amazonica

A comprehensive Clojure client for the entire [Amazon AWS api] [1].      
## Installation  

Add the following dependency to your Clojure project:

    [amazonica "0.1.0"]    
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
Amazonica supports the complete set of remote service calls implemented by each of the 
AWS service client classes (Amazon*Client.java), the documentation for which can be found 
in the [AWS Javadocs] [2].

## Example Usage
&nbsp;  

###EC2
```clojure
(ns com.example
  (:use (amazonica core ec2)))
        
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