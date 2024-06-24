(ns amazonica.core
  "Amazon AWS functions."
  (:use [clojure.algo.generic.functor :only (fmap)])
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import clojure.lang.Reflector
           [com.amazonaws
             AmazonServiceException
             SdkClientException
             ClientConfiguration]
           [com.amazonaws.auth
             AWSCredentials
             AWSCredentialsProvider
             AWSStaticCredentialsProvider
             BasicAWSCredentials
             BasicSessionCredentials
             DefaultAWSCredentialsProviderChain]
           [com.amazonaws.util
             AwsHostNameUtils]
           com.amazonaws.auth.profile.ProfileCredentialsProvider
           [com.amazonaws.regions
             Region
             Regions
             DefaultAwsRegionProviderChain]
           com.amazonaws.client.builder.AwsClientBuilder
           com.amazonaws.client.builder.AwsClientBuilder$EndpointConfiguration
           org.joda.time.DateTime
           org.joda.time.base.AbstractInstant
           java.io.File
           java.io.PrintWriter
           java.io.StringWriter
           java.lang.reflect.InvocationTargetException
           java.lang.reflect.ParameterizedType
           java.lang.reflect.Method
           java.lang.reflect.Modifier
           java.lang.reflect.Constructor
           java.lang.reflect.Parameter
           java.math.BigDecimal
           java.math.BigInteger
           java.nio.ByteBuffer
           java.text.ParsePosition
           java.text.SimpleDateFormat
           java.util.Date))

(defonce ^:private credential (atom {}))

(defonce ^:private client-config (atom {}))

(def ^:dynamic ^:private *credentials* nil)

(def ^:dynamic ^:private *client-config* nil)

(def ^:dynamic ^:private *client-class* nil)

(def ^:private date-format (atom "yyyy-MM-dd"))

(def ^:private root-unwrapping (atom false))

(defn- invoke-constructor
  [class-name arg-vec]
  (Reflector/invokeConstructor
    (Class/forName class-name)
    (into-array Object arg-vec)))

(defn set-root-unwrapping!
  "Enables JSON-like root unwrapping of singly keyed
  top level maps.
    {:root {:key 'foo' :name 'bar'}}
  would become
    {:key 'foo' :name 'bar'}"
  [b]
  (reset! root-unwrapping b))

(defn set-date-format!
  "Sets the java.text.SimpleDateFormat pattern to use
  for transparent coercion of Strings passed as
  arguments where java.util.Dates are required by the
  AWS api."
  [df]
  (reset! date-format df))


(defn stack->string
  "Converts a Java stacktrace to String representation."
  [^Throwable ex]
  (let [sw (StringWriter.)
        pw (PrintWriter. sw)
        _  (.printStackTrace ex pw)]
    (.toString sw)))

(defn ex->map
  "Converts a com.amazonaws.AmazonServiceException to a
  Clojure map with keys:
  :error-code
  :error-type
  :status-code
  :request-id
  :service-name
  :message
  :stack-trace"
  [^AmazonServiceException e]
  {:error-code   (.getErrorCode e)
   :error-type   (.toString (.getErrorType e))
   :status-code  (.getStatusCode e)
   :request-id   (.getRequestId e)
   :service-name (.getServiceName e)
   :message      (.getMessage e)
   :stack-trace  (stack->string e)})

(defn keys->cred
  [access-key secret-key & [endpoint]]
  (let [credential {:access-key access-key
                    :secret-key secret-key}]
    (if-not (empty? endpoint)
      (merge credential {:endpoint endpoint})
      credential)))

(defn defcredential
  "Specify the AWS access key, secret key and optional
  endpoint to use on subsequent requests."
  ([cred]
   (reset! credential cred))
  ([access-key secret-key & [endpoint]]
   (defcredential (keys->cred access-key secret-key endpoint))))

(defn defclientconfig
  "Specify the default Client configuration to use on
  subsequent requests."
  [config]
  (reset! client-config config))

(defmacro with-credential
  "Per invocation binding of credentials for ad-hoc
  service calls using alternate user/password combos
  (and endpoints)."
  [cred & body]
  `(binding [*credentials*
             (let [cred# ~cred]
               (if (sequential? cred#)
                 (apply keys->cred cred#)
                 cred#))]
    (do ~@body)))

(defmacro with-client-config
  "Per invocation binding of client-config for ad-hoc
  service calls using alternate client configuration."
  [config & body]
  `(binding [*client-config* ~config]
     (do ~@body)))


(defn- get-default-region
  "Get the AWS region
   
   Takes AWS_DEFAULT_REGION as the first priority to match old behaviour. Otherwise, see 
   https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html#default-region-provider-chain
   for region selection order.
   
   Returns nil if no region is defined."
  []
  (or
   (System/getenv "AWS_DEFAULT_REGION")
   (try
     (.getRegion (DefaultAwsRegionProviderChain.))
     (catch SdkClientException _
       nil))))

(defn- builder ^AwsClientBuilder
  ([^Class clazz]
   (builder clazz "builder"))
  ([^Class clazz builder-method]
   (let [^Method method (.getMethod clazz builder-method (make-array Class 0))]
     (.invoke method clazz (make-array Object 0)))))

(declare set-fields)

(defn- build-client [^Class clazz credentials configuration raw-creds options]
  (let [default-region (get-default-region)
        builder (builder clazz)
        _ (set-fields builder options)
        builder (if credentials (.withCredentials builder credentials) builder)
        builder (if configuration (.withClientConfiguration builder configuration) builder)
        ^String endpoint (or (:endpoint raw-creds) default-region)
        builder (if endpoint
                  (if (.startsWith endpoint "http")
                    (.withEndpointConfiguration
                     builder
                     (AwsClientBuilder$EndpointConfiguration. endpoint (or (AwsHostNameUtils/parseRegion endpoint nil) default-region)))
                    (.withRegion builder endpoint))
                  builder)]
    (.build builder)))

(defn- create-client
  [^Class clazz credentials configuration raw-creds options]
  (if (= (.getSimpleName clazz) "TransferManager")
    (invoke-constructor
      "com.amazonaws.services.s3.transfer.TransferManager"
      [(create-client (Class/forName "com.amazonaws.services.s3.AmazonS3Client")
                      credentials
                      configuration
                      raw-creds {})])
    (build-client clazz credentials configuration raw-creds options)))

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
    (ProfileCredentialsProvider.
        (:profile credentials))
    (and (associative? credentials)
         (instance? AWSCredentialsProvider (:cred credentials)))
    (:cred credentials)
    :else
    (DefaultAWSCredentialsProviderChain/getInstance)))

(defn parse-args
  "Legacy support means credentials may or may not be passed
   as the first argument."
  [cred args]
  (if (and (instance? AWSCredentialsProvider (get-credentials cred))
           (not (instance? AWSCredentialsProvider cred))
           (nil? (:endpoint cred))
           (nil? (:profile cred)))
    {:args (conj args cred)}
    {:args args :cred cred}))

(declare create-bean)

(defn- get-client-configuration
  [configuration]
  (when (associative? configuration)
    (create-bean ClientConfiguration configuration)))

(defn- get-region ^Regions
  [credentials]
  (when-let [endpoint (or (:endpoint credentials)
                          (get-default-region))]
    (if (contains? (fmap #(-> % str/upper-case (str/replace "_" ""))
                         (apply hash-set (seq (Regions/values))))
                   (-> (str/upper-case endpoint)
                       (str/replace "-" "")))
        (try
          (->> (-> (str/upper-case endpoint)
                   (str/replace "-" "_"))
               Regions/valueOf)
          (catch NoSuchMethodException e
            (println e))))))

(defn- build-encryption-client [^Class clazz credentials configuration raw-creds options materials crypto]
  (let [default-region (get-default-region)
        credentials (get-credentials raw-creds)
        builder (builder clazz "encryptionBuilder")
        _ (set-fields builder options)
        builder (if credentials (.withCredentials builder credentials) builder)
        builder (if configuration (.withClientConfiguration builder configuration) builder)
        builder (if materials (.withEncryptionMaterials builder materials) builder)
        builder (if crypto (.withCryptoConfiguration builder crypto) builder)
        ^String endpoint (or (:endpoint raw-creds) default-region)
        builder (if endpoint
                  (if (.startsWith endpoint "http")
                    (.withEndpointConfiguration
                     builder
                     (AwsClientBuilder$EndpointConfiguration. endpoint (or (AwsHostNameUtils/parseRegion endpoint nil) default-region)))
                    (.withRegion builder endpoint))
                  builder)]
    (.build builder)))

(defn encryption-client*
  [encryption credentials configuration]
  (let [creds     (get-credentials credentials)
        config    (get-client-configuration configuration)
        key       (or (:secret-key encryption)
                      (:key-pair encryption)
                      (:kms-customer-master-key encryption))
        crypto    (invoke-constructor
                    "com.amazonaws.services.s3.model.CryptoConfiguration" [])
        _         (when (:kms-customer-master-key encryption)
                    (.setAwsKmsRegion crypto (Region/getRegion (get-region {:endpoint (:region key)})) ))
        em        (when-not (:kms-customer-master-key encryption)
                    (invoke-constructor "com.amazonaws.services.s3.model.EncryptionMaterials"
                    [key]))
        materials (if (:kms-customer-master-key encryption)
                    (invoke-constructor "com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider"
                    [(:id key)])
                    (invoke-constructor
                    "com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider"
                    [em]))
        _         (if-let [provider (:provider encryption)]
                    (.withCryptoProvider crypto provider))
        clazz     (Class/forName "com.amazonaws.services.s3.AmazonS3EncryptionClient")]
    (build-encryption-client clazz creds config credentials configuration materials crypto)))

(swap! client-config assoc :encryption-client-fn (memoize encryption-client*))

(defn amazon-client*
  [clazz credentials configuration]
  (let [aws-creds  (get-credentials credentials)
        aws-config (get-client-configuration configuration)
        client     (create-client clazz aws-creds aws-config credentials configuration)]
    client))

(swap! client-config assoc :amazon-client-fn (memoize amazon-client*))

(defn- keyword-converter
  "Given something that tokenizes a string into parts, turn it into
  a :kebab-case-keyword."
  [separator-regex]
  (fn [s]
    (->> (str/split s separator-regex)
         (map str/lower-case)
         (interpose \-)
         str/join
         keyword)))

(def ^:private camel->keyword
  "from Emerick, Grande, Carper 2012 p.70"
  (keyword-converter #"(?<=[a-z])(?=[A-Z])"))

(def ^:private camel->keyword2
  "Like [[camel->keyword]], but smarter about acronyms and concepts like iSCSI
  and OpenID which should be treated as a single concept."
  (comp
   (keyword-converter #"(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")
   #(str/replace % #"(?i)iscsi" "ISCSI")
   #(str/replace % #"(?i)openid" "OPENID")))

(defn- ^String keyword->camel
  [kw]
  (let [n (name kw)
        m (.replace n "?" "")]
    (->> (str/split m #"\-")
         (fmap str/capitalize)
         str/join)))

(defn- aws-package?
  [^Class clazz]
  (->> (.getName clazz)
       (re-find #"com\.amazonaws\.services")
       some?))

(defn to-date
  [date]
  (cond
    (instance? java.util.Date date) date
    (instance? AbstractInstant date) (.toDate ^AbstractInstant date)
    (integer? date) (java.util.Date. (long date))
    true (.. (SimpleDateFormat. @date-format)
           (parse (str date) (ParsePosition. 0)))))

(defn to-file
  [file]
  (if (instance? File file)
    file
    (if (string? file)
      (File. ^String file))))

(defn to-enum
  "Case-insensitive resolution of Enum types by String."
  ^java.lang.Enum [^Class type value]
  (some
    (fn [^java.lang.Enum en]
      (if (and
            (not (nil? (.toString en))) ; some aws enums return nil!
            (= (str/upper-case value)
               (str/upper-case (.toString en))))
        en))
    (-> type
        (.getDeclaredMethod "values" (make-array Class 0))
        (.invoke type (make-array Object 0)))))

; assoc java Class to Clojure cast functions
(defonce ^:private coercions
  (->> [:String :Integer :Long :Double :Float]
       (reduce
         (fn [m e]
           (let [arr (str "[Ljava.lang." (name e) ";")
                 clazz (Class/forName arr)]
             (assoc m clazz into-array))) {})
       (merge {
         String     str
         Integer    int
         Long       long
         Boolean    boolean
         Double     double
         Float      float
         BigDecimal bigdec
         BigInteger bigint
         Date       to-date
         File       to-file
         Region     #(Region/getRegion (Regions/fromName %))
         ByteBuffer #(-> % str .getBytes ByteBuffer/wrap)
         "int"      int
         "long"     long
         "double"   double
         "float"    float
         "boolean"  boolean})
       atom))

(defn register-coercions
  "Accepts key/value pairs of class/function, which defines
  how data will be converted to the appropriate type
  required by the AWS Amazon*Client Java method."
  [& {:as coercion}]
  (swap! coercions merge coercion))

(defn coerce-value
  "Coerces the supplied stringvalue to the required type as
  defined by the AWS method signature. String or keyword
  conversion to Enum types (e.g. via valueOf()) is supported."
  [value ^Class type]
  (let [value (if (keyword? value) (name value) value)]
    (if-not (instance? type value)
      (if (= java.lang.Enum (.getSuperclass type))
        (to-enum type value)
        (if-let [coercion (@coercions (if (.isPrimitive type)
                                        (str type)
                                        type))]
          (coercion value)
          (throw (IllegalArgumentException.
                   (format "No coercion is available to turn %s into an object of type %s"
                           value type)))))
      value)))

(defn- default-value
  [class-name]
  (get
    {"boolean" false
     "double" (double 0.0)
     "float" (float 0.0)
     "long" 0
     "int" (int 0)}
    class-name))

(defn- constructor-args
  [^Constructor ctor]
  (let [types (.getParameterTypes ctor)]
    (if (= 0 (count types))
      (make-array Object 0)
      (into-array Object
        (map
          (comp default-value str)
          (seq types))))))

(defn best-constructor
  "Prefer no-arg ctor if one exists, else the first found."
  ^Constructor [^Class clazz]
  {:pre [clazz]}
  (let [ctors (.getConstructors clazz)]
    (or
      (some
        (fn [^Constructor ctor]
          (when (= 0 (count (.getParameterTypes ctor))) ctor))
        ctors)
      (first (sort-by
               (fn [^Constructor ctor]
                 (some aws-package? (.getParameterTypes ctor)))
               ctors)))))

(defn- new-instance
  "Create a new instance of a Java bean. S3 neccessitates
  the check for contructor args here, as the rest of the
  AWS api contains strictly no-arg ctor JavaBeans."
  [clazz]
  {:pre [clazz]}
  (let [ctor (best-constructor clazz)
        arr  (constructor-args ctor)]
    (.newInstance ctor arr)))

(defn- unwind-types
  [depth param]
  (if (instance? ParameterizedType param)
      (let [f (partial unwind-types (inc depth))
            types (-> ^ParameterizedType param .getActualTypeArguments)
            t (-> types last f)]
        (if (and (instance? ParameterizedType (last types))
                 (.contains (-> types last str) "java.util")
                 (.contains (-> types last str) "java.lang"))
          {:type [(-> types ^ParameterizedType last .getRawType)]
           :depth depth}
          t))
      {:type [param]
       :depth depth}))

(defn- paramter-types
  [^Method method]
  (let [types (seq (.getGenericParameterTypes method))
        param (last types)
        rval  {:generic types}]
    (if (instance? ParameterizedType param)
        (let [t (unwind-types 1 param)]
          (merge rval
                 {:actual (:type t)
                  :depth  (:depth t)}))
        rval)))

(defn- normalized-name
  [^String method-name]
  (-> method-name
      (.replaceFirst
        (case (.substring method-name 0 3)
          "get" "get"
          "set" "set"
          "default")
      "")
      (.toLowerCase)))

(defn- matches?
  "We exclude any mutators of the bean which
   expect a java.util.Map$Entry as the first
   argument, as we won't be dealing in these
   from Clojure. Specifically, this is meant
   to address the various setKey() methods
   in the DynamoDBV2Client.
   http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/DeleteItemRequest.html#setKey(java.util.Map.Entry, java.util.Map.Entry)
   http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/GetItemRequest.html#setKey(java.util.Map.Entry, java.util.Map.Entry)"
  [^Method method name value]
  (let [args (.getParameterTypes method)
        n (count args)
        ^Class clazz (first args)]
    (and (= name (normalized-name (.getName method)))
         (if (empty? value)
             (= 0 n)
             (and (< 0 n)
                  (or (= n (count (flatten value)))
                      (and (coll? value)
                           (= 1 n)
                           (or (contains? @coercions clazz)
                               (.isArray clazz)
                               (and (.getPackage clazz)
                                    (.startsWith
                                      (.getName (.getPackage clazz))
                                      "java.util")))))
                  (not (and (= 2 n)
                            (every? (partial = java.util.Map$Entry)
                                    args))))))))

(defn- accessor-methods
  [class-methods name value]
  (reduce
    #(if (matches? %2 name value)
      (conj %1 %2)
      %1)
    []
    class-methods))

(defn- find-methods
  [^Object pojo k & v]
  (-> (.getClass pojo)
      (.getMethods)
      (accessor-methods
        (.toLowerCase (keyword->camel k))
        v)))

(defn to-java-coll
  "Need this only because S3 methods actually try to
   mutate (e.g. sort) collections passed to them."
  [col]
  (cond
    (map? col)
      (doto
        (java.util.HashMap.)
        (.putAll col))
    (set? col)
      (java.util.HashSet. ^java.util.Set col)
    (or (list? col) (vector? col))
      (java.util.ArrayList. ^java.util.Collection col)))

(defn- invoke
  [pojo ^Method method v]
  (.invoke method pojo
    (into-array
      ;; misbehaving S3Client mutates the coll
      (if (and (coll? v)
               (not (nil? *client-class*))
               (= "AmazonS3Client" (.getSimpleName ^Class *client-class*)))
        (if (and (> (count (.getParameterTypes method)) 1)
                 (sequential? v))
          (to-java-coll v)
          [(to-java-coll v)])
          [v])))
  true)

(defn kw->str [k]
  (if (keyword? k) (name k) k))

(defn- populate
  [types key props]
  (let [type (-> types key last)]
    (assert type (str "Bad data type - there is no " key " in " types))
    (if (contains? @coercions type)
      (coerce-value props type)
      (set-fields (new-instance type) props))))

(defn- unmarshall
  "Transform Clojure data to the required Java objects."
  [types col]
  (let [^Class type (last (or (:actual types)
                              (:generic types)))
        pp   (partial populate types :actual)]
    (try
      (if (aws-package? type)
       (if (map? col)
         (if (contains? types :actual)
           (if (empty? col)
             {}  ; assoc doesn't like being called to do nothing
             (if (< (:depth types) 3)
               (apply assoc {}
                      (interleave (fmap kw->str (apply vector (keys col)))
                                  (fmap pp (apply vector (vals col)))))
               (apply assoc {}
                      (interleave (fmap kw->str (apply vector (keys col)))
                                  [(fmap #(populate {:generic [type]}
                                                    :generic
                                                    %)
                                         (first (apply vector (vals col))))]))))
           (populate types :generic col))
         (if (and (contains? types :actual)
                  (= (:depth types) 3))
           (fmap #(fmap pp %) col)
           (fmap pp col)))
       (if (and (contains? types :actual)
                (aws-package? type))
         (fmap pp col)
         (fmap #(coerce-value % type) col)))
      (catch Throwable e
        (throw (RuntimeException. (str
                                    "Failed to create an instance of "
                                    (.getName type)
                                    " from " col
                                    " due to " e
                                    ". Make sure the data matches an existing constructor and setters.")))))))

(defn- invoke-method
  [pojo v method]
  (let [f       (partial invoke pojo method)
        types   (paramter-types method)
        generic (last (:generic types))]
    (if (and
          (coll? v)
          (not (contains? @coercions generic)))
      (f (unmarshall types v))
      (if (instance? generic v)
        (f v)
        (f (coerce-value v generic))))))

(defn set-fields
  "Returns the populated AWS *Request bean with 'args' as
   the values. args is a map with keywords as keys and any
   type values. Complex values will be recursively resolved
   to the corresponding method calls on the object graph."
  [pojo args]
  (doseq [[k v] args]
    (try
      (->> (find-methods pojo k v)
           (some (partial invoke-method pojo v)))
      (catch Throwable e
        (throw (ex-info
                 (str "Error setting " k ": " (.getMessage e) ". Perhaps the value isn't compatible with the setter?")
                 {:property k, :value v}
                 e)))))
  pojo)

(defn- create-bean
  [clazz args]
  (-> clazz new-instance (set-fields args)))

(defn- create-request-bean
  "Create a new instance of an AWS *Request style Java
   bean passed as the argument to a method call on the
   Amazon*Client class. (Note that we assume all AWS
   service calls take at most a single argument.)"
  [^Method method args]
  (let [clazz (first (.getParameterTypes method))]
    (if (contains? @coercions clazz)
        (coerce-value (into {} args) clazz)
        (create-bean clazz args))))


(defprotocol IMarshall
  "Defines the contract for converting Java types to Clojure
  data. All return values from AWS service calls are
  marshalled. As such, the AWS service-specific namespaces
  will frequently need to implement this protocol in order
  to provide convenient data representations. See also the
  register-coercions function for coercing Clojure data to
  Java types."
  (marshall [obj]))

(defn- getter?
  [^Method method]
  (let [name (.getName method)
        type (.getName (.getReturnType method))]
    (or (and
          (.startsWith name "get")
          (= 0 (count (.getParameterTypes method))))
        (and
          (.startsWith name "is")
          (= "boolean" type)))))

(defn accessors
  "Returns a vector of getters or setters for the class."
  [^Class clazz getters?]
  (reduce
    (fn [acc ^Method m]
      (if (or
            (and getters? (getter? m))
            (and (not getters?)
                 (.startsWith (.getName m) "set")))
        (conj acc m)
        acc))
    []
    (.getDeclaredMethods clazz)))

(defn- prop->name
  [^Method method]
  (let [name (.getName method)]
    (if (.startsWith name "is")
      (str (.substring name 2) "?")
      (.substring name 3))))

(defn get-fields
  "Returns a map of all non-null values returned by
  invoking all public getters on the specified object."
  [obj]
  (let [no-arg (make-array Object 0)]
    (into {}
      (for [^Method m (accessors (class obj) true)]
        (let [r (marshall (.invoke m obj no-arg))]
          (if-not (nil? r)
            (hash-map
              (camel->keyword (prop->name m))
              r)))))))

(extend-protocol IMarshall
  nil
  (marshall [obj] nil)

  java.util.Map
  (marshall [obj]
    (if-not (empty? obj)
      (apply assoc {}
        (interleave
          (fmap #(if (string? %) (keyword %) %)
                (apply vector (keys obj)))
          (fmap marshall
                (apply vector (vals obj)))))))

  java.util.Collection
  (marshall [obj]
    (if (instance? clojure.lang.IPersistentSet obj)
      obj
      (fmap marshall (apply vector obj))))

  java.util.Date
  (marshall [obj] (DateTime. (.getTime obj)))

  ; `false` boolean objects (i.e. (Boolean. false)) come out of e.g.
  ; .doesBucketExist, which wreak havoc on Clojure truthiness
  Boolean
  (marshall [obj] (when-not (nil? obj) (.booleanValue obj)))

  Object
  (marshall [obj]
    (if (aws-package? (class obj))
        (get-fields obj)
        obj)))

(defn- use-aws-request-bean?
  [^Method method args]
  (let [types (.getParameterTypes method)]
    (and (or (map? args) (< 1 (count args)) (nil? args))
         (< 0 (count types))
         (or (nil? args)
             (map? args)
             (and
                (or (and
                      (even? (count args))
                      (not= java.io.File (last types)))
                    (and
                      (odd? (count args))
                      (= java.io.File (last types)))) ; s3 getObject() support
                (some keyword? args)))
         (or (aws-package? (first types))
             (and (aws-package? (last types))
                  (not (< (count types) (count args))))))))

(defn- prepare-args
  [^Method method args]
  (let [types (.getParameterTypes method)
        num   (count types)]
    (if (and (empty? args) (= 0 num))
      (into-array Object args)
      (if (= num (count args))
        (into-array Object
                    (map (fn [arg ^Class clazz]
                           (if (and (aws-package? clazz) (seq (.getConstructors clazz)))
                             ; must be a concrete, instantiatable class
                             (if (contains? @coercions clazz)
                               (coerce-value arg clazz)
                               (create-bean clazz arg))
                             (coerce-value arg clazz)))
                         (vec args)
                         types))
        (if (use-aws-request-bean? method args)
          (cond
            (= 1 num)
            (into-array Object
                        [(create-request-bean
                           method
                           (seq (apply hash-map args)))])
            (and (aws-package? (first types))
                 (= 2 num)
                 (= File (last types)))
            (into-array Object
                        [(create-request-bean
                           method
                           (seq (apply hash-map (butlast args))))
                         (last args)])))))))


(defn- args-from
  "Function arguments take an optional first parameter map
  of AWS credentials. Addtional parameters are either a map,
  or seq of keys and values."
  [arg-seq]
  (let [args (first arg-seq)]
    (cond
      (or (and (or (map? args)
                   (map? (first args)))
               (or (contains? (first args) :access-key)
                   (contains? (first args) :endpoint)
                   (contains? (first args) :profile)
                   (contains? (first args) :client-config)))
          (instance? AWSCredentialsProvider (first args))
          (instance? AWSCredentials (first args)))
      {:args (if (-> args rest first map?)
                 (if (-> args rest first empty?)
                     {}
                     (mapcat identity (-> args rest args-from :args)))
                 (rest args))
       :credential (if (map? (first args))
                       (dissoc (first args) :client-config)
                       (first args))
       :client-config (:client-config (first args))}
      (map? (first args))
      {:args (let [m (mapcat identity (first args))]
               (if (seq m) m {}))}
      :default {:args args})))

(defn transfer-manager*
  [credential client-config crypto]
  (let [encryption-client (:encryption-client-fn client-config)
        amazon-client (:amazon-client-fn client-config)]
    (invoke-constructor
      "com.amazonaws.services.s3.transfer.TransferManager"
      (if crypto
        [(encryption-client crypto credential client-config)]
        [(amazon-client (Class/forName "com.amazonaws.services.s3.AmazonS3Client")
                        credential
                        client-config)]))))

(swap! client-config assoc :transfer-manager-fn (memoize transfer-manager*))

(defn candidate-client
  [^Class clazz args]
  (let [cred-bound (or *credentials* (:credential args))
        credential (if (map? cred-bound)
                             (merge @credential cred-bound)
                             (or cred-bound @credential))
        config-bound (or *client-config* (:client-config args))
        client-config (merge @client-config config-bound)
        encryption-client (:encryption-client-fn client-config)
        amazon-client (:amazon-client-fn client-config)
        transfer-manager (:transfer-manager-fn client-config)
        crypto (if (even? (count (:args args)))
                   (:encryption (apply hash-map (:args args))))
        client  (if (and crypto (or (= (.getSimpleName clazz) "AmazonS3Client")
                                    (= (.getSimpleName clazz) "TransferManager")))
                    (delay (encryption-client crypto credential client-config))
                    (delay (amazon-client clazz credential client-config)))]
        (if (= (.getSimpleName clazz) "TransferManager")
            (transfer-manager credential client-config crypto)
            @client)))


(defn- fn-call
  "Returns a function that reflectively invokes method on
   clazz with supplied args (if any). The 'method' here is
   the Java method on the Amazon*Client class."
  [clazz ^Method method & arg]
  (binding [*client-class* clazz]
    (let [args    (args-from arg)
          arg-arr (prepare-args method (:args args))
          client  (delay (candidate-client clazz args))]
      (fn []
        (try
          (let [java (.invoke method @client arg-arr)
                cloj (marshall java)]
            (if (and
                  @root-unwrapping
                  (map? cloj)
                  (= 1 (count (keys cloj))))
              (-> cloj first second)
              cloj))
          (catch InvocationTargetException ite
            (throw (.getTargetException ite))))))))

(defn- types-match-args
  [args ^Method method]
  (let [types (.getParameterTypes method)]
    (if (and (= (count types) (count args))
             (every? identity (map instance? types args)))
        method)))

(defn- coercible? [type]
  (and (contains? @coercions type)
       (not (re-find #"java\.lang" (str type)))))

(defn- choose-from [possible]
  (if (= 1 (count possible))
      (first possible)
      (first
        (sort-by
          (fn [^Method method]
            (let [types (.getParameterTypes method)]
              (cond
                (some coercible? types) 1
                (some #(= java.lang.Enum (.getSuperclass ^Class %)) types) 2
                :else 3)))
          possible))))

(defn possible-methods
  [methods args]
  (filter
    (fn [^Method method]
      (let [types (.getParameterTypes method)
            num   (count types)]
        (if (or
              (and (empty? args) (= 0 num))
              (use-aws-request-bean? method args)
              (and
                (= num (count args))
                (not (keyword? (first args)))
                (not (aws-package? (first types)))))
          method
          false)))
    methods))

(defn- best-method
  "Finds the appropriate method to invoke in cases where
  the Amazon*Client has overloaded methods by arity or type."
  [methods & arg]
  (let [args (:args (args-from arg))]
    (or (some (partial types-match-args args) methods)
        (choose-from (possible-methods methods args)))))

(defn- clojure-case
  "Similar to \"kabob case\" but the returned string is suitable for
  reading a single symbol with `read-string`."
  [string]
  (-> string
      ;; Replace the space between a non-upper-case letter and an
      ;; upper-case letter with a dash.
      (str/replace #"(?<=[^A-Z])(?=[A-Z])" "-")
      ;; Remove anything that a Clojure reader would not accept in a
      ;; symbol.
      (str/replace #"[\\'\"\[\]\(\){}\s]" "")
      (str/lower-case)))

(defn- type-clojure-name
  "Given a `java.lang.Class` return it's name in kabob case"
  [^Class type]
  (let [type-name (last (.. type getName (split "\\.")))
        type-name (if-let [;; Check for a type name like "[C" etc.
                           [_ name] (re-matches #"\[([A-Z]+)$" type-name)]
                    name
                    type-name)]
    (clojure-case type-name)))

(defn- parameter-clojure-name
  "Given a `java.lang.reflect.Parameter` return it's name in kabob
  case."
  [^Parameter parameter]
  (if (. parameter isNamePresent)
    (clojure-case (. parameter getName))
    ;; The name will be synthesized so instead we'll derive
    ;; it from it's type.
    (type-clojure-name (. parameter getType))))

(def ^{:arglists '([method])
       :private true}
  parameter-names
  "Given a `java.lang.reflect.Method` return a list of it's parameter
  names."
  ;; The regular expression here will only match against version
  ;; numbers that are 1.7.X and below.
  (if (re-matches #"[^2-9]\.[1-7]\..+" (System/getProperty "java.version"))
    ;; Java 1.7 and below.
    (fn [^Method method]
      (map type-clojure-name (. method getParameterTypes)))
    ;; Java 1.8 and above.
    (fn [^Method method]
      (map parameter-clojure-name (. method getParameters)))))

(defn- method-arglist
  "Derives a Clojure `:arglist` vector from a
  `java.lang.reflect.Method`."
  [^Method method]
  (let [names (parameter-names method)
        ^Parameter first-parameter (some-> method .getParameters first)
        ^Class type (when first-parameter (.getType first-parameter))
        fields (when (class? type)
                 (try
                   (->> type
                        .newInstance
                        bean
                        (remove (comp #{:class} first))
                        (into {})
                        (keys)
                        (map (comp camel->keyword2 name))
                        (map (comp symbol name))
                        ;; fields common to all requests (calculated via `set/intersection`)
                        (remove '#{clone-source request-credentials-provider request-metric-collector clone-root custom-request-headers sdk-client-execution-timeout request-credentials sdk-request-timeout custom-query-parameters request-client-options read-limit general-progress-listener})
                        (sort)
                        (vec))
                   (catch InstantiationException _)))
        ;; This will help determine when parameter names should be
        ;; suffixed with an index i.e. `parameter-1`. Suffixing is
        ;; necessary when parameter names are synthesized from their
        ;; type names and the likelihood duplicates is high.
        name-frequency (frequencies names)]
    (loop [names names
           ;; This map keeps track of the index of names when they
           ;; appear more than once.
           name-index {}
           arglist []]
      (if (empty? names)
        (cond
          (empty? arglist)
          arglist

          (= 1 (count arglist))
          ['& (if fields
                (assoc {:keys fields} ;; awkward construction for keeping a nice order when querying the arglist interactively (:keys will show up first this way)
                       :as            (first arglist))
                arglist)]

          true
          arglist)

        (let [[name & names*] names]
          (if (= (name-frequency name) 1)
            (let [arg-symbol (symbol name)
                  arglist* (conj arglist arg-symbol)]
              (recur names*
                     name-index
                     arglist*))
            ;; The parameter name appears more than once so we need to
            ;; attach an index to it and update our name-index for the
            ;; next parameter with the same name.
            (let [index (get name-index name 1)
                  name-index* (assoc name-index name (inc index))
                  arg-symbol (symbol (str name "-" index))
                  arglist* (conj arglist arg-symbol)]
              (recur names*
                     name-index*
                     arglist*))))))))

(defn intern-function
  "Interns into ns, the symbol mapped to a Clojure function
   derived from the java.lang.reflect.Method(s). Overloaded
   methods will yield a variadic Clojure function."
  [client ns fname methods]
  (let [source (some-> client pr-str munge (str/replace "." "/") (str ".java") (io/resource) str)
        ^Method first-method (first methods)]
    (intern ns (with-meta (symbol (name fname))
                 (cond-> {:amazonica/client  client
                          :amazonica/methods methods
                          :amazonica/method-name (.getName first-method)
                          :arglists          (sort-by pr-str (map method-arglist methods))}
                   source (assoc :amazonica/source source)))
            (fn [& args]
              (if-let [method (best-method methods args)]
                (if-not args
                  ((fn-call client method))
                  ((fn-call client method args)))
                (throw (IllegalArgumentException.
                        (format "Could not determine best method to invoke for %s using arguments %s"
                                (name fname) args))))))))

(defn- client-methods
  "Returns a map with keys of idiomatic Clojure hyphenated keywords
  corresponding to the public Java method names of the class argument, vals are
  vectors of java.lang.reflect.Methods."
  [^Class client]
  (->> (.getDeclaredMethods client)
       (remove (fn [^Method method]
                 (let [mods (.getModifiers method)]
                   (or (not (Modifier/isPublic mods))
                       (Modifier/isStatic mods)
                       (.isSynthetic method)))))
       (group-by #(camel->keyword (.getName ^Method %)))))

(defn- show-functions [ns]
  (intern ns (symbol "show-functions")
    (fn []
      (->> (ns-publics ns)
           sort
           (map (comp println first))))))

(defn set-client
  "Intern into the specified namespace all public methods
   from the Amazon*Client class as Clojure functions."
  [client ns]
  (show-functions ns)
  (intern ns 'client-class client)
  (doseq [[fname methods] (client-methods client)
          :let [the-var (intern-function client ns fname methods)
                fname2 (-> methods ^Method first .getName camel->keyword2)]]
    (when (not= fname fname2)
      (let [the-var2 (intern-function client ns fname2 methods)]
        (alter-meta! the-var assoc :amazonica/deprecated-in-favor-of the-var2)))))
