(ns amazonica.test.ec2
  (:import com.amazonaws.services.ec2.model.RunInstancesRequest)
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.aws.ec2]))

(deftest ec2 []
  
  (def vpc-id
   (-> (create-vpc :cidr-block "10.0.0.0/16") :vpc :vpc-id))


  (def network-acl-id
    (-> (create-network-acl :vpc-id vpc-id) :network-acl :network-acl-id))

  (clojure.pprint/pprint
    (create-network-acl-entry :network-acl-id network-acl-id
                              :cidr-block "0.0.0.0/0"
                              :egress false
                              :port-range [22 22]
                              :rule-action "deny"
                              :protocol "6"
                              :rule-number 100))

  (clojure.pprint/pprint
    (describe-network-acls :network-aclids [network-acl-id]))


  (delete-network-acl-entry :network-acl-id network-acl-id
                            :egress false
                            :rule-number 100)
  (delete-network-acl :network-acl-id network-acl-id)
  (delete-vpc :vpc-id vpc-id)

  (clojure.pprint/pprint
    (describe-availability-zones))

  (clojure.pprint/pprint
    (describe-dhcp-options))

  #_(clojure.pprint/pprint
    (describe-images :owners ["self"]))

  (clojure.pprint/pprint
    (describe-instances))

  ; (let [image-id (create-image :name "my_test_image"
  ;                              :instance-id "i-1b9a9f71"
  ;                              :description "test image - safe to delete"
  ;                              :block-device-mappings [
  ;                                {:device-name  "/dev/sda1"
  ;                                 :virtual-name "myvirtual"
  ;                                 :ebs {
  ;                                   :volume-size 8
  ;                                   :volume-type "standard"
  ;                                   :delete-on-termination true}}])]
  ;   (deregister-image :image-id (:image-id image-id)))
    ;(deregister-image :image-id "ami-f00f9699")
    
  ;; test for marshalling map values 
  ;; see https://github.com/mcohen01/amazonica/issues/219
  (let [pojo (RunInstancesRequest.)]
    (amazonica.core/set-fields pojo {:block-device-mappings [{:device-name "foobar"}]})
    (is (= "foobar"
           (-> pojo .getBlockDeviceMappings first .getDeviceName))))
  
)
