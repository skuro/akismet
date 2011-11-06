(ns akismet
  "Clojure library to call Akismet services.

   In order for all calls to work properly, you need to rebind the following vars:

   - `*akismet-key*` the Akismet API key to use. Get one [here](https://akismet.com/signup/)
   - `*akismet-blog*` the full URL of the blog you registered you API key for

   The main API is composed by:

   - `valid-key?` a predicate that checks Akismet API key validity.
   - `spam?` checks whether the provided comment is spam.
   - `spam!` informs Akismet of a spam comment it missed. Helps improving spam detection for your blog.
   - `ham! informs Akismet of a comment it wrongly detected as spam. Helps improving spam detection for your blog.`"  
  (:require [clj-http.client :as client]))

(def ^:dynamic *akismet-key*)

(def ^:dynamic *akismet-blog*)

(defonce base-url ".rest.akismet.com/1.1/")

(defn api-call-body
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

(defn- spam-internal
  "Common implementation for most of the Akismet API calls

   - api    : keyword, one of [:verify-key :check-comment :submit-spam :submit-ham]
   - params : map of all available parameters to be included as a POST body
              in the API call"
  [api {:keys [user_ip
               user_agent
               referrer
               permalink
               comment_type
               comment_author
               comment_author_email
               comment_author_url
               comment_content] :as params}]
  ;; check for mandatory parameters:
  {:pre [(not-any? nil? [user_ip user_agent])]}
  (call-akismet api (merge {:blog *akismet-blog*} params)))

(defn valid-key?
  "Verifies the API key to use when calling Akismet services.
   Returns true if the key is valid, false otherwise."
  []
  (= "valid" (call-akismet :verify-key {:key *akismet-key*
                                        :blog *akismet-blog*})))

(defn spam?
  "Asks Akismet to evaluate whether the provided text is spam.
   Returns true if the comment is found to be spam, false otherwise.
   params must contain at least:
   - :user_id
   - :user_agent
   See spam-internal for more options"
  [params]
  (Boolean/parseBoolean (spam-internal :comment-check params)))

(defn spam!
  "Notifies Akismet of a spam comment it missed.
   params must contain at least:
   - :user_id
   - :user_agent
   See spam-internal for more options"
  [params]
  (spam-internal :submit-spam params))

(defn ham!
  "Notified Akismet of a comment wrongly marked as spam. 
   params must contain at least:
   - :user_id
   - :user_agent
   See spam-internal for more options"
  [params]
  (spam-internal :submit-ham params))
