(ns amazonica.test.route53
  (:use [clojure.test]
        [amazonica.aws.route53]))

(deftest route53 []

  (create-health-check
    :health-check-config
      {:port 80,
       :type "HTTP",
       :ipaddress "127.0.0.1",
       :fully-qualified-domain-name "example.com"})

  (get-health-check :health-check-id "ce6a4aeb-acf1-4923-a116-cd9ae2c30ee3")

  (create-hosted-zone :name "example.com.")

  (get-hosted-zone :id "Z3TKY0VR5CH45U")
  
  (list-hosted-zones)

  (list-health-checks)

  (list-resource-record-sets :hosted-zone-id "ZN8D0HXQLVRRL")

  (delete-health-check :health-check-id "99999999-1234-4923-a116-cd9ae2c30ee3")

  (delete-hosted-zone :id "my-bogus-hosted-zone")

)