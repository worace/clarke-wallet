(ns clarke-wallet.views
  (:require [cljs.core.async :as a]
            [reagent.core :as r]))

(defn wallet-list [app-state events]
  [:div
   [:h3 "Local Wallets"]
   [:ul (map (fn [w] [:li (str w)]) (:wallets app-state))]])

(defn prompt-for-wallet [app-state events]
  (let [wallet-name (r/atom "default")]
    [:div [:h3 "No wallet found. Create one?"]
     [:label "Wallet Name:"]
     [:input {:type "text"
              :value @wallet-name
              :on-change #(reset! wallet-name (-> % .-target .-value))}]
     [:input {:type "button" :value "Make Wallet!"
              :on-click #(a/put! events {:event :create-wallet
                                         :data @wallet-name})}]]))

(defn wallets [app-state events]
  (case (:wallet-status app-state)
     :not-loaded [:h3 "Loading Wallet..."]
     :empty (prompt-for-wallet app-state events)
     :generating [:h3 "Generating Wallet..."]
     :ready (wallet-list app-state events)))

(defn node-list [app-state events]
  [:div
    [:h2 "Connected Nodes"]
    [:ul (map (fn [n] [:li (str n)]) (:nodes app-state))]])

(defn main [app-state events]
  [:div
   (wallets app-state events)
   (node-list app-state events)])

(defn make-wallet [app-state events]
  [:div "MAKE A WALLET"])
