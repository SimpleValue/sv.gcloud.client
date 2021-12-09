(ns sv.gcloud.client
  (:import com.google.api.client.googleapis.auth.oauth2.GoogleCredential)
  (:require [clj-http.client :as c]))

(defn wrap-access-token
  "Uses `GoogleCredential` to request and manage access tokens to
   authorize requests to GCP APIs. Automatically refreshes expired
   access-tokens.

   Via the `config` map it is possible to define which
   `:scopes` (https://developers.google.com/identity/protocols/oauth2/scopes)
   should be requested. Furthermore you can provide your own
   `com.google.api.client.googleapis.auth.oauth2.GoogleCredential`
   instance via the `:credential` config map entry."
  [client config]
  (let [credential (:credential config
                                (GoogleCredential/getApplicationDefault))
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

(defn create-client
  "Creates a clj-http client (see `clj-http.client/request`) for
   accessing Google Cloud Platform (GCP) APIs. It uses the
   ApplicationDefault mechanism of the Google Cloud SDK to receive
   credentials for the current instance.

   GCP instances automatically provide their credentials via this
   mechanism, while Google Cloud SDK installations provide the
   credentials of the last successful `gcloud auth login` (or
   the configured service account credentials).

   It is recommended to wrap this client with the
   `sv.gcloud.client.middleware.retry/wrap-retry` middleware to retry
   failed requests which are retryable (see
   https://cloud.google.com/storage/docs/gsutil/addlhelp/RetryHandlingStrategy)."
  [config]
  (wrap-access-token
   c/request
   config))
