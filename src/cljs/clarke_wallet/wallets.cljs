(ns clarke-wallet.wallets
  (:require [cljs.core.async :as a]))

(def node-rsa (js/require "node-rsa"))
(def fs (js/require "fs"))
(def fse (js/require "fs-extra"))

(defn slurp [path] (.readFileSync fs path))
(def home-dir (-> js/process .-env .-HOME))
(def wallet-directory (str home-dir "/.clarke-coin/wallets"))
(defn init-wallet-dir! [] (.ensureDirSync fse wallet-directory))

(defn public-der [key]
  (-> key
      (.exportKey "pkcs8-public-der")
      (.toString "base64")))

(defn load-wallet [path]
  (let [data (slurp path)
        kp (doto (node-rsa)
             (.importKey data "pkcs8"))]
    {:address (public-der kp)
     :key kp}))

;; (def wallet-der (atom nil))
;; (.readFile fs
;;            "/Users/worace/.wallet.der"
;;            "utf8"
;;            (fn [err data]
;;              (reset! wallet-der data)
;;              (-> (doto (node-rsa) (.importKey @wallet-der "pkcs8"))
;;                  (.sign (js/Buffer. "pizza") "base64"))))

(defn list-files [dir]
  (-> fs (.readdirSync dir) (js->clj)))

(defn load-wallets [channel]
  (println "WILL LOAD WALLETS...")
  (init-wallet-dir!)
  (doseq [f (list-files wallet-directory)]
    (println "loading wallet at file:" f)
    (a/put! channel {:event :wallet-loaded
                     :data (load-wallet f)}))
  (a/put! channel {:event :wallets-loaded :data nil}))
