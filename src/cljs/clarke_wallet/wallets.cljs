(ns clarke-wallet.wallets
  (:require [cljs.core.async :as a]))

(def node-rsa (js/require "node-rsa"))
(def fs (js/require "fs"))
(def fse (js/require "fs-extra"))
(def ipc (.-ipcRenderer (js/require "electron")))

(defn slurp [path] (.readFileSync fs path))
(def home-dir (-> js/process .-env .-HOME))
(def wallet-directory (str home-dir "/.clarke-coin/wallets"))
(defn init-wallet-dir! [] (.ensureDirSync fse wallet-directory))

(defn public-der [key]
  (-> key
      (.exportKey "pkcs8-public-der")
      (.toString "base64")))

(defn pkcs8-pem [key]
  (-> key
      (.exportKey "pkcs8")
      (.toString "base64")
      ))

(defn private-der [key]
  (->> (clojure.string/split (pkcs8-pem key) "\n")
       (drop 1)
       (drop-last 1)
       (clojure.string/join "")))

(defn wallet-name [filepath]
  (-> filepath
      (clojure.string/split "/")
      (last)
      (clojure.string/replace #".der" "")))

(defn load-wallet [path]
  (let [data (slurp path)
        kp (doto (node-rsa)
             (.importKey data "pkcs8"))]
    {:address (public-der kp)
     :name (wallet-name path)
     :key kp}))

(defn list-files [dir]
  (-> fs (.readdirSync dir) (js->clj)))

(defn load-wallets [channel]
  (init-wallet-dir!)
  (doseq [f (list-files wallet-directory)]
    (a/put! channel {:event :wallet-loaded
                     :data (load-wallet (str wallet-directory "/" f))}))
  (a/put! channel {:event :wallets-loaded :data nil}))

(defn write-file [path data]
  (.writeFileSync fs path data))

(defn make-wallet [file-name channel]
  (let [kp (node-rsa {:b 2048})]
    (write-file (str wallet-directory "/" file-name ".der")
                (private-der kp))
    (a/put! channel {:event :wallet-loaded
                     :data {:address (public-der kp)
                            :key kp
                            :name file-name}})))


(defn cat-keys
  "take a map and a vector of keys and create a concatenated
   string of the value for each key"
  [keys m]
  (apply str (map (partial get m) keys)))

(def input-signable (partial cat-keys [:source-hash :source-index]))
(def input-hashable (partial cat-keys [:source-hash :source-index]))
(def output-signable (partial cat-keys [:amount :address]))
(def output-hashable output-signable)

(defn txn-signable [txn]
  (apply str (concat (map input-signable (:inputs txn))
                     (map output-signable (:outputs txn))
                     [(:min-height txn) (:timestamp txn)])))

(defn sign [key string]
  (.toString (.sign key (js/Buffer. string) "buffer" "base64")
             "base64"))

(defn sign-payment [key-map txn]
  (println "SIGNABLE")
  (println (txn-signable txn))
  (println (sign (:key key-map) (txn-signable txn)))
  (let [sig (sign (:key key-map) (txn-signable txn))]
    (assoc txn
           :inputs
           (->> (:inputs txn)
                (map (fn [i] (assoc i :signature sig)))
                (vector)))))
