(ns amazonica.test.ec2
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.core]
        [amazonica.aws.ec2]))

(deftest ec2 []

  (clojure.pprint/pprint
    (describe-availability-zones))

  (clojure.pprint/pprint
    (describe-dhcp-options))

  #_(clojure.pprint/pprint
    (describe-images :owners ["self"]))

  (clojure.pprint/pprint
    (describe-instances))

  (let [image-id (create-image :name "my_test_image"
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
