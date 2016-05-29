(ns clarke-wallet.core
  (:require [reagent.core :as reagent]
            [cljsjs.react]))

(enable-console-print!)

(defonce app-state {:wallet-key nil
                    :wallet-addr nil
                    :nodes #{}})

(def wallet-dir "/var/lib/clarke-coin/wallets")

(defn main-page
  []
  [:div "Hello Pizza!"])

(defn mount-root
  []
  (reagent/render [main-page] (.getElementById js/document "app")))

(defn init!
  []
  (println "HI")
  (mount-root))

(def node-rsa (js/require "node-rsa"))
(def fs (js/require "fs"))
(def wallet-der (atom nil))

(.readFile fs
           "/home/worace/.wallet.der"
           "utf8"
           (fn [err data]
             (println err data)
             (reset! wallet-der data)))

(def keypair
  (doto (node-rsa) (.importKey @wallet-der "pkcs8")))

(.sign keypair (js/Buffer. "pizza") "base64")

;; (.sign (doto (node-rsa)
;;          (.importKey @wallet-der "pkcs8")) (js/Buffer. "pizza") "base64")

;; Booting
;; Read files in wallet dir
;; if none, prompt to create
;; connect to DNSServer to find available nodes
;; fetch balance for wallet addr
