(ns amazonica.test.route53
  (:use [clojure.test]
        [clojure.pprint]
        [amazonica.core]
        [amazonica.aws.route53]))

; config file contains space-separated AWS credential key pair
; and optional third param of AWS endpoint (e.g. for different
; region than the default US_East)
(def cred 
  (apply 
    hash-map 
      (interleave 
        [:access-key :secret-key :endpoint]
        (seq (.split (slurp "aws.config") " ")))))

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