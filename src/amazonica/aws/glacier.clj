(ns amazonica.aws.glacier
  (:use [amazonica.core :only (get-credentials set-client to-file)]
        [robert.hooke :only (add-hook)])
  (:import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
           [com.amazonaws.services.glacier
              AmazonGlacierClient]
           [amazonica TreeHash]
           [java.io
              BufferedInputStream
              FileInputStream]))

(set-client AmazonGlacierClient *ns*)

(defn- file-hash->map
  [file]
  {:content-length (.length file)
   :checksum       (-> file
                       TreeHash/computeSHA256TreeHash
                       TreeHash/toHex)
   :body           (-> file
                       (FileInputStream.) 
                       (BufferedInputStream.))})

(defn parse-args
  "Legacy support means credentials may or may not be passed
   as the first argument."
  [cred args]
  (if (instance? DefaultAWSCredentialsProviderChain (get-credentials cred))
      {:args (conj args cred)}
      {:args args :cred cred}))

(defn tree-hash
  [f cred & args]
  (let [arg-map (parse-args cred args)
        func    (if (contains? arg-map :cred)
                    (partial f cred)
                    f)
        m       (apply hash-map (:args arg-map))
        file    (to-file (:body m))        
        mm      (merge-with               
                  (fn [_ e] e)
                  m
                  (file-hash->map file))]
    (apply func (mapcat identity mm))))

(add-hook #'upload-archive #'tree-hash)