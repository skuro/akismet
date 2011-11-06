(ns akismet.core
  (:require [clj-http.client :as client]
            [clojure.string :as str]))

(def ^:dynamic *akismet-key*)

(defonce base-url ".rest.akismet.com/1.1/")

(defn- api-call-body
  [params]
  {:content-type :x-www-form-urlencoded
   :headers {"User-Agent" "Development/1.0 | akismet/1.0-SNAPSHOT"}
   :body (client/generate-query-string params)})

(defn call-akismet
  "Generic method to invoke Akismet APIs.
api is one of :verify-key, :comment-check, :submit-spam or :submit-ham"
  [api params]
  {:pre [(#{:verify-key
            :comment-check
            :submit-spam
            :submit-ham} api)]}
  (let [url (str "http://" *akismet-key* base-url (name api))]
    (:body (client/post url (api-call-body params)))))

(defn valid-key?
  "Verifies the API key to use when calling Akismet services.
Returns true if the key is valid, false otherwise."
  [blog]
  {:pre [(not (nil? blog))]}
  (= "valid" (call-akismet :verify-key {:key *akismet-key*
                                        :blog blog})))

(defn- spam-internal
  [api blog {:keys [user_ip
                    user_agent
                    referrer
                    permalink
                    comment_type
                    comment_author
                    comment_author_email
                    comment_author_url
                    comment_content] :as params}]
  ;; check for mandatory parameters:
  {:pre [(not-any? nil? [blog user_ip user_agent])]}
  (call-akismet api (merge {:blog blog} params)))

(defn spam?
  "Asks Akismet to evaluate whether the provided text is spam.
Returns true if the comment is found to be spam, false otherwise"
  [blog params]
  (Boolean/parseBoolean (spam-internal :comment-check blog params)))

(defn spam!
  "Notifies Akismet of a spam comment it missed"
  [blog params]
  (spam-internal :submit-spam))

(defn ham!
  "Notified Akismet of a comment wrongly marked as spam"
  [blog params]
  (spam-internal :submit-ham))

;; TODO: create a function that extracts akismet params from HTTP req
(defn wrap-akismet
  "Ring middleware that allows automatic submission to Akismet.

   applies? is a predicate fn used to filter requests for which
            akismet checks apply

   extract-text is a fn that extracts the comment from the request

   blog is the HTTP address of the blog

   key is the akismet API key"
  [app blog key applies? extract-text]
  (fn [req]
    (binding [*akismet-key* key]
              (let [req* (if (applies? req)
                           (assoc req :suspect-spam (spam? blog req))
                           req)
                    ret (app req*)]
                (if (:submit-spam ret)
                  (spam! blog req)
                  (if (:submit-ham ret)
                    (ham! blog req)))))))
