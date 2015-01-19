(ns amazonica.test.route53
  (:use [clojure.test]
        [amazonica.aws.route53]))

(deftest route53 []

  (let [config {:caller-reference (str (java.util.UUID/randomUUID))
                :health-check-config
                  {:port 80,
                   :type "HTTP",
                   :ipaddress "93.184.216.34",
                   :fully-qualified-domain-name "example.com"}}
        {check :health-check} (create-health-check config)]
    
    (get-health-check :health-check-id (:id check))

    (is (-> (list-health-checks)
            :health-checks
            count
            (> 0)))
    
    (delete-health-check :health-check-id (:id check)))
                                        
  
  (let [{zone :hosted-zone} (create-hosted-zone
                              :name "example69.com"
                              :caller-reference (str (java.util.UUID/randomUUID)))]
    
    (get-hosted-zone :id (:id zone))
    
    (list-hosted-zones)
    
    (list-resource-record-sets :hosted-zone-id (:id zone))
    
    (delete-hosted-zone :id (:id zone)))

)