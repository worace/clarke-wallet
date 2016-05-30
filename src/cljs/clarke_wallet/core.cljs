(ns clarke-wallet.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as reagent]
            [clarke-wallet.wallets :as w]
            [clarke-wallet.views :as views]
            [clarke-wallet.http :as http]
            [cljs.core.async :as a]
            [cljsjs.react]))
(def fs (js/require "fs"))
(def fse (js/require "fs-extra"))

(enable-console-print!)

(defonce event-channel (a/chan))
(defonce app-state (reagent/atom {:wallets {}
                                  :current-view :main
                                  :address-book {}
                                  :current-wallet nil
                                  :wallet-status :not-loaded
                                  :nodes #{}}))

(defn read-json [string] (js->clj (.parse js/JSON string)))
(defn write-json [value] (.stringify js/JSON (clj->js value)))

(def home-dir (-> js/process .-env .-HOME))
(def clarke-coin-dir (str home-dir "/.clarke-coin"))
(def address-book-path (str clarke-coin-dir "/addresses.json"))

(defn write-file [path string]
  (let [c (a/chan)]
    (.writeFile
     fs
     path
     string
     (fn [err] (when-not (nil? err) (a/put! c err))))
    c))

(defn ui
  []
  (case (:current-view @app-state)
    (views/main @app-state event-channel)))

(defn some-node []
  (if (empty? (:nodes @app-state))
    nil
   (rand-nth (into [] (:nodes @app-state)))))

(defn refresh-wallet-balance [wallet]
  (go (if-let [n (some-node)]
        (swap! app-state
               assoc-in
               [:wallets (:name wallet) :balance]
               (->> wallet
                    :address
                    (http/wallet-balance n)
                    (a/<!)
                    :body
                    :payload
                    :balance)))))

(defn store-address [addr]
  (go (swap! app-state assoc-in [:address-book (:name addr)] addr)
      (if-let [e (a/<! (write-file address-book-path
                                   (write-json (:address-book @app-state))))]
        (println "Error writing address book:" e))))

(defn event-received [{event :event data :data :as e}]
  (case event
    :wallets-loaded (if (empty? (:wallets @app-state))
                      (swap! app-state assoc :wallet-status :empty)
                      (swap! app-state assoc :wallet-status :ready))
    :wallet-loaded (do (swap! app-state update :wallets assoc (:name data) data)
                       (swap! app-state assoc :wallet-status :ready)
                       (refresh-wallet-balance data))
    :wallet-selected (do (swap! app-state assoc :current-wallet (:name data))
                         (refresh-wallet-balance data))
    :store-address (store-address data)
    :delete-address (swap! app-state update :address-book dissoc (:name data))
    :create-wallet (do (swap! app-state assoc :wallet-status :generating)
                       (w/make-wallet data event-channel))
    (println "Unknown event type:" e)))

(defn run-event-loop [event-chan]
  (go-loop []
      (when-let [event (a/<! event-chan)]
        (println "Received event" event)
        (event-received event)
      (println "state" @app-state)
      (recur))))

(defn mount-root
  []
  (reagent/render [ui] (.getElementById js/document "app")))

(defn load-available-nodes []
  (go (doseq [p (:body (a/<! (http/fetch-nodes)))]
        (swap! app-state update :nodes conj p))))

(defn read-file [path]
  (let [c (a/chan)]
    (.readFile
     fs
     path
     (fn [err contents]
       (if (nil? err)
         (a/put! c (str contents))
         (a/close! c))))
    c))

(defn keyword-keys [some-map]
  (->> some-map
       (map (fn [[k v]] [(keyword k) v]))
       (into {})))

(defn read-address-book []
  (let [c (a/chan)]
    (go (a/put! c (if-let [val (a/<! (read-file address-book-path))]
                    (reduce (fn [r [k nested-map]]
                              (assoc r k (keyword-keys nested-map)))
                            {}
                            (read-json val))
                    {})))
    c))

(defn load-address-book []
  (go (swap! app-state assoc :address-book (a/<! (read-address-book)))))

(defn init!
  []
  (run-event-loop event-channel)
  (load-available-nodes)
  (load-address-book)
  (mount-root)
  (w/load-wallets event-channel))

