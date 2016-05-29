(ns clarke-wallet.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as reagent]
            [clarke-wallet.wallets :as w]
            [clarke-wallet.views :as views]
            [clarke-wallet.http :as http]
            [cljs.core.async :as a]
            [cljsjs.react]))

(enable-console-print!)

(defonce event-channel (a/chan))
(defonce app-state (reagent/atom {:wallets []
                                  :current-view :main
                                  :current-wallet nil
                                  :wallet-status :not-loaded
                                  :nodes #{}}))

(defn ui
  []
  (case (:current-view @app-state)
    (views/main @app-state event-channel)))

(defn event-received [{event :event data :data :as e}]
  (case event
    :wallets-loaded (if (empty? (:wallets @app-state))
                      (swap! app-state assoc :wallet-status :empty)
                      (swap! app-state assoc :wallet-status :ready))
    :wallet-loaded (do (swap! app-state update :wallets conj data)
                       (swap! app-state assoc :wallet-status :ready))
    :wallet-selected (swap! app-state assoc :current-wallet data)
    :create-wallet (do (println "WILL MAKE WALLET")
                       (swap! app-state assoc :wallet-status :generating)
                       (println "SET GENERATING")
                       (w/make-wallet data event-channel)
                       (println "FINISHED GO block"))
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

(defn init!
  []
  (run-event-loop event-channel)
  (load-available-nodes)
  (mount-root)
  (w/load-wallets event-channel))

