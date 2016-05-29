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
