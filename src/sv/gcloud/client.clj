(ns sv.gcloud.client
  "Helps to interact with the REST APIs of Google Cloud and most of
   Google's other REST APIs.

   See this page for a list of possible OAuth2 scopes:

   https://developers.google.com/identity/protocols/oauth2/scopes
  "
  (:import com.google.api.client.googleapis.auth.oauth2.GoogleCredential)
  (:require [clj-http.client :as c]))

(defn application-default-get-access-token
  "Uses `GoogleCredential` to request and manage access tokens to
   authorize requests to GCP APIs. Automatically refreshes expired
   access-tokens.

   Via the `config` map it is possible to define which
   `:scopes` (https://developers.google.com/identity/protocols/oauth2/scopes)
   should be requested.

   Perform `gcloud auth application-default login` to make this work
   on your development machine."
  [config]
  (let [credential (GoogleCredential/getApplicationDefault)
        credential (if-let [scopes (:scopes config)]
                     (.createScoped credential scopes)
                     credential)]
    (fn []
      (when (or (not (.getAccessToken credential))
                (> (System/currentTimeMillis)
                   (-
                    (.getExpirationTimeMilliseconds
                     credential)
                    (* 1000 60 10))))
        (.refreshToken credential))
      (.getAccessToken credential))
    ))

(defn- add-access-token
  [request access-token]
  (assoc-in request
            [:headers "Authorization"]
            (str "Bearer " access-token)))

(defn wrap-access-token
  "By default uses `application-default-get-access-token` to get an access-token fo r Google Cloud.

   Via the `:get-access-token` `config` map entry it is possible to
   provide an alternative no-arg function that gets an access-token."
  [client config]
  (let [get-access-token (or (:get-access-token config)
                             (application-default-get-access-token config))]
    (fn [request]
      (client (add-access-token request
                                (get-access-token))))))

(defn- impersonate-get-access-token-request
  [{:keys [service-account scopes]}]
  {:request-method :post
   :url (format "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/%s:generateAccessToken"
                service-account)
   :as :json
   :content-type :json
   :form-params {:scope scopes}})

(defn impersonate-get-access-token
  "Allows to impersonate as a service account. Thereby it is not
   necessary to download and store a key.json of the service account on
   your development machine.

   When you use `gcloud auth application-default login` pay attention
   that the selected user has the IAM role 'Service Account Token
   Creator' / 'roles/iam.serviceAccountTokenCreator' assigned.

   In the `config` map provide the `:service-account` email and the
   OAuth2 `:scopes`."
  [{:keys [client] :as config}]
  (let [client (or client
                   (wrap-access-token c/request
                                      {}))]
    (fn []
      (let [request (impersonate-get-access-token-request config)
            response (client request)]
        (get-in response
                [:body
                 :accessToken])))))

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
