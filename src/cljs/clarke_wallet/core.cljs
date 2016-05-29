(ns clarke-wallet.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as reagent]
            [clarke-wallet.wallets :as w]
            [clarke-wallet.views :as views]
            [cljs.core.async :as a]
            [cljsjs.react]))

(enable-console-print!)

(defonce event-channel (a/chan))
(defonce app-state (reagent/atom {:wallets []
                                  :current-view :main
                                  :wallet-status :not-loaded
                                  :nodes #{{:host "159.203.206.49" :port 3000} {:host "159.203.206.63" :port 3000}}}))

(defn ui
  []
  (case (:current-view @app-state)
    (views/main @app-state event-channel)))

(defn event-received [{event :event data :data :as e}]
  (case event
    :wallets-loaded (if (empty? (:wallets @app-state))
                      (swap! app-state assoc :wallet-status :empty)
                      (swap! app-state assoc :wallets-loaded :loaded))
    :wallet-loaded (do (swap! app-state update :wallets conj data)
                       (swap! app-state assoc :wallet-status :loaded))
    :create-wallet (swap! app-state assoc :generating-wallet true)
    (println "Unknown event type:" e)))

(defn run-event-loop [event-chan]
  (go-loop []
      (when-let [event (a/<! event-chan)]
        (println (event-received event))
        (println "GOT EVENT" event))
      (println "state" @app-state)
      (recur)))

(defn mount-root
  []
  (reagent/render [ui] (.getElementById js/document "app")))

(defn init!
  []
  (run-event-loop event-channel)
  (mount-root)
  (w/load-wallets event-channel))

(def http (js/require "http"))

(defn read-json [string]
  (->> string
       (.parse js/JSON)
       (js->clj)))

(defn http-get [host path cb]
  (println "HTTP Get" host path)
  (.get http (clj->js {:host host :path path})
        (fn [response]
          (println "recv resp" response)
          (let [body (atom [])]
            (.on response "data" (fn [d]
                                   (swap! body conj d)))
            (.on response "error" (fn [e] println e))
            (.on response "end" (fn []
                                  (cb (read-json (apply str @body)))))))))

;; (http-get "dns1.clarkecoin.org" "/api/peers" println)
