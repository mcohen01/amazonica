(ns amazonica.credentials
  (:require [clojure.java.shell])
  (:import [com.amazonaws.auth.profile.internal
            BasicProfileConfigLoader
            AwsProfileNameLoader]
           [com.amazonaws.auth.profile
            ProfilesConfigFile
            ProfileCredentialsProvider]
           [com.amazonaws.auth
            AWSCredentials
            AWSCredentialsProvider
            AWSCredentialsProviderChain
            AWSStaticCredentialsProvider
            BasicAWSCredentials
            BasicSessionCredentials
            DefaultAWSCredentialsProviderChain
            EC2ContainerCredentialsProviderWrapper
            EnvironmentVariableCredentialsProvider            
            SystemPropertiesCredentialsProvider]))

(defn load-profile-name []
  ;; As in https://github.com/aws/aws-sdk-java/blob/05a142018a82825c680f7176b21fde064ff7b8ab/aws-java-sdk-core/src/main/java/com/amazonaws/auth/profile/ProfileCredentialsProvider.java#L124
  (.loadProfileName (AwsProfileNameLoader/INSTANCE)))

(defn load-profile [profile-name]
  ;; As in https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/auth/profile/ProfilesConfigFile.java#L177
  (get (.getAllBasicProfiles (ProfilesConfigFile.)) profile-name))

(defn get-credentials-process-cmd [profile]
  ;; See https://docs.aws.amazon.com/cli/latest/topic/config-vars.html#sourcing-credentials-from-external-processes
  (get (.getProperties profile) "credential_process"))

(defn run-credential-process-cmd [cmd]
  (let [cmd (clojure.string/split cmd #" ")
        {:keys [exit out err]} (apply clojure.java.shell/sh cmd)]
    (if (zero? exit)
      out
      (throw (ex-info (str "Non-zero exit: " (pr-str err)) {})))))

(defn parse-credentials-json [json]
  ;; The credential json has a simple structure and we only need string values
  ;; No need to include an extra dependency on a json parser
  (apply hash-map (-> json
                      (clojure.string/replace #"[}{,:\"]" "")
                      (clojure.string/split #"\s+"))))

(defn get-credentials-via-cmd [cmd]
  (let [json (run-credential-process-cmd cmd)
        credential-map (parse-credentials-json json)
        {:strs [AccessKeyId SecretAccessKey SessionToken]} credential-map]
    (assert (and AccessKeyId SecretAccessKey SessionToken))
    (BasicSessionCredentials. AccessKeyId
                              SecretAccessKey
                              SessionToken)))

(defn extended-profile-credentials-provider ^AWSCredentialsProvider
  ([]
   (extended-profile-credentials-provider (load-profile-name)))
  ([profile-name]
   (let [provider (ProfileCredentialsProvider. profile-name)]
     (reify AWSCredentialsProvider
       (getCredentials [_]
         (try
           (.getCredentials provider)
           (catch com.amazonaws.SdkClientException e
             (if-let [profile (load-profile profile-name)]
               (if-let [cmd (get-credentials-process-cmd profile)]
                 (get-credentials-via-cmd cmd)
                 ;; Re-throw, there is no credential_process field either
                 (throw e))
               ;; Re-throw, profile doesn't exist
               (throw e)))))))))

(defn extended-default-credentials-provider-chain ^AWSCredentialsProvider []
  ;; As in https://github.com/aws/aws-sdk-java/blob/34acb557b674b157d854d1e6d1d583256d8fefd1/aws-java-sdk-core/src/main/java/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.java
  (AWSCredentialsProviderChain.
   (java.util.ArrayList. [(EnvironmentVariableCredentialsProvider.)
                          (SystemPropertiesCredentialsProvider.)
                          (extended-profile-credentials-provider)
                          (EC2ContainerCredentialsProviderWrapper.)])))

(defn get-credentials ^AWSCredentialsProvider
  [credentials]
  (cond
    (instance? AWSCredentialsProvider credentials)
      credentials
    (instance? AWSCredentials credentials)
      (AWSStaticCredentialsProvider. credentials)
    (and (associative? credentials)
         (contains? credentials :session-token))
    (AWSStaticCredentialsProvider.
      (BasicSessionCredentials.
        (:access-key credentials)
        (:secret-key credentials)
        (:session-token credentials)))
    (and (associative? credentials)
         (contains? credentials :access-key))
    (AWSStaticCredentialsProvider.
      (BasicAWSCredentials.
        (:access-key credentials)
        (:secret-key credentials)))
    (and (associative? credentials)
         (contains? credentials :profile))
    (extended-profile-credentials-provider
      (:profile credentials))
    (and (associative? credentials)
         (instance? AWSCredentialsProvider (:cred credentials)))
    (:cred credentials)
    :else
    (extended-default-credentials-provider-chain)))


(comment

  (.getCredentials (extended-profile-credentials-provider))
  )
