(defproject amazonica "0.2.0"
  :description "A comprehensive Clojure client for the entire Amazon AWS api."
  :url "https://github.com/mcohen01/amazonica"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :java-source-paths ["src/main/java"]
  :target-path "target"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/algo.generic "0.1.0"]
                 [joda-time "2.2"]
                 [robert/hooke "1.3.0"]
                 [com.taoensso/nippy "2.5.0"]
                 
                 [local/aws-java-sdk "1.6.4"]
                 [local/kinesis-client-lib "1.0"]                 
                 [com.fasterxml.jackson.core/jackson-core "2.1.1"]
                 [com.fasterxml.jackson.core/jackson-annotations "2.1.1"]
                 [com.fasterxml.jackson.core/jackson-databind "2.1.1"]
                 [commons-logging "1.1.1"]
                 [commons-codec "1.3"]
                 [commons-lang "2.4"]
                 [org.apache.httpcomponents/httpclient "4.2"]
                 [org.apache.httpcomponents/httpcore "4.2"]])
