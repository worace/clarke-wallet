(ns clarke-wallet.http
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a]
            [clojure.string :refer [starts-with?]]
            [cljs-http.client :as http]))

(defn url [node path]
  (str "http://"
       (:host node)
       ":"
       (:port node)
       (if (starts-with? path "/")
         path
         (str "/" path))))

(defn wallet-balance [node wallet-addr]
  (http/post (url node "balance")
             {:json-params {:address wallet-addr}}))

(defn fetch-nodes []
  (http/get "http://dns1.clarkecoin.org/api/peers"))

(defn request-unsigned-txn [node {:keys [from-address to-address amount fee]}]
  (http/post (url node "unsigned_payment_transactions")
             {:json-params {:from_address from-address
                            :to_address to-address
                            :amount (js/parseInt (or amount 0))
                            :fee (js/parseInt (or fee 0))}}))

(defn submit-signed-payment [node txn]
  (http/post (url node "pending_transactions")
             {:json-params txn}))
