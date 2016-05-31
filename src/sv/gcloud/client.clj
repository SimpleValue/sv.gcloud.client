(ns sv.gcloud.client
  (:import com.google.api.client.googleapis.auth.oauth2.GoogleCredential)
  (:require [clj-http.client :as c]))

(defn wrap-access-token [client config]
  (let [credential (GoogleCredential/getApplicationDefault)
        credential (if-let [scopes (:scopes config)]
                     (.createScoped credential scopes)
                     credential)]
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
        [:headers "Authorization"]
        (str "Bearer " (.getAccessToken credential)))))))

(defn create-client [config]
  (wrap-access-token
   c/request
   config))
