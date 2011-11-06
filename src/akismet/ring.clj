(ns akismet.ring
  (:use [akismet]))

(defn extract-text-default
  "Default implementation to extract comment text from a request"
  [req]
  (:comment req))

(defn wrap-akismet
  "Ring middleware that allows automatic submission to Akismet.

   applies? is a predicate fn used to filter requests for which
            akismet checks apply

   extract-text is a fn that extracts the comment from the request"
  ([app applies?]
     (wrap-akismet app applies? extract-text-default))
  ([app applies? extract-text]
     (fn [req]
       (let [req* (if (applies? req)
                    (assoc req :suspect-spam (spam? req))
                    req)
             ret (app req*)]
         (if (:submit-spam ret)
           (spam! req)
           (if (:submit-ham ret)
             (ham! req)))))))
