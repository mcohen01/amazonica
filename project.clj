(defproject amazonica "0.3.114"
  :description "A comprehensive Clojure client for the entire Amazon AWS api."
  :url "https://github.com/mcohen01/amazonica"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :java-source-paths ["src/main/java"]
  :javac-options ["-target" "1.6" "-source" "1.6"]
  :target-path "target"
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/algo.generic "0.1.2"]
                 [com.amazonaws/aws-java-sdk "1.12.132" :exclusions [joda-time]]
                 [com.amazonaws/amazon-kinesis-client "1.14.7" :exclusions [joda-time]]
                 [com.amazonaws/dynamodb-streams-kinesis-adapter "1.2.1"
                  :exclusions [com.amazonaws/amazon-kinesis-client
                               com.amazonaws/aws-java-sdk-cloudwatch
                               com.amazonaws/aws-java-sdk-dynamodb
                               com.amazonaws/aws-java-sdk-kinesis
                               joda-time]]
                 [joda-time "2.9.6"]
                 [robert/hooke "1.3.0"]
                 [com.taoensso/nippy "2.12.2"]])
