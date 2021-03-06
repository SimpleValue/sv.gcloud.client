(defproject sv/gcloud.client "0.1.5-SNAPSHOT"
  :description "A client to talk to the Google Cloud API (based on clj-http)."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-http "2.0.0"]
                 [cheshire "5.5.0"]
                 [listora/again "0.1.0"]
                 [com.google.api-client/google-api-client "1.28.0"]])
