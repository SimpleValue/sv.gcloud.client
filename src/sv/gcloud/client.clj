(ns sv.gcloud.client
  (:import com.google.api.client.googleapis.auth.oauth2.GoogleCredential)
  (:require [clj-http.client :as c]))

(defonce credential (GoogleCredential/getApplicationDefault))

(defn wrap-access-token [client]
  (fn [request]
    (when (or (not (.getAccessToken credential))
              (> (System/currentTimeMillis)
                 (-
                  (.getExpirationTimeMilliseconds
                   credential)
                  (* 1000 60 10))))
      (.refreshToken credential))
    (client
     (assoc-in
      request
      [:query-params :access_token]
      (.getAccessToken credential)))))

(def client
  (wrap-access-token
   c/request))
