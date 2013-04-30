(ns amazonica.test.ec2
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.core]
        [amazonica.aws.ec2]))

; config file contains space-separated AWS credential key pair
; and optional third param of AWS endpoint (e.g. for different
; region than the default US_East)
(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))

(deftest ec2 []

  (clojure.pprint/pprint
    (describe-availability-zones cred))

  (clojure.pprint/pprint
    (describe-dhcp-options cred))

  #_(clojure.pprint/pprint
    (describe-images cred :owners ["self"]))

  (clojure.pprint/pprint
    (describe-instances cred))

  (let [image-id (create-image cred
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
    (deregister-image cred :image-id (:image-id image-id)))
    ;(deregister-image :image-id "ami-f00f9699")
)
