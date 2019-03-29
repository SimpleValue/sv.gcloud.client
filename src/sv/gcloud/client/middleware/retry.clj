(ns sv.gcloud.client.middleware.retry
  (:require [again.core :as again]))

(defn default-retry-strategy
  "The default retry strategy: an exponential-backoff with a
   max-duration of 10 seconds and a maximum of 10 retries."
  []
  (again/max-duration
   10000
   (again/max-retries
    10
    (again/randomize-strategy
     0.5
     (again/multiplicative-strategy 500 1.5)))))

(def http-status-too-many-requests 429)

(defn http-status-5xx?
  "Is the status code 5xx (500-599)"
  [status-code]
  (<= 500 status-code 599))

(defn http-status-retry?
  "Is the status code either a 5xx or a `too many request` (429)."
  [status-code]
  (or
   (= status-code http-status-too-many-requests)
   (http-status-5xx? status-code)))

(defn retry?
  "Default `retry?` for `wrap-retry` middleware. Is the status code
   of the response either a 5xx or a `too many request` (429)."
  [request response]
  (http-status-retry? (:status response)))

(defn- sleep
  [delay]
  (Thread/sleep (long delay)))

(defn wrap-retry
  "Middleware that retries the request, if (retry? request response)
   returns true. The retry-strategy function should return a sequence
   of millisecond amounts, for example: `[200 1000]` means wait 200 ms
   after the first and 1 second after the second request.

   Read more about retryable request in the context of Google Cloud
   Platform here:

   https://cloud.google.com/storage/docs/gsutil/addlhelp/RetryHandlingStrategy"
  ([client retry-strategy retry?]
   (fn [request]
     (loop [retries (cons 0 (retry-strategy))]
       (let [sleep-ms (first retries)]
         (sleep sleep-ms)
         (let [response (client request)]
           (if (and (true? (retry? request response))
                    (seq (rest retries)))
             (recur (rest retries))
             response))))))
  ([client retry-strategy]
   (wrap-retry client retry-strategy retry?))
  ([client]
   (wrap-retry client default-retry-strategy)))
