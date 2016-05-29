(ns clarke-wallet.views
  (:require [cljs.core.async :as a]
            [reagent.core :as r]))

(defn wallet-li [app-state events wallet]
  [:li [:b
        {:on-click #(a/put! events {:event :wallet-selected
                                    :data wallet})
         :style {:text-decoration "underline"}}
        (:name wallet)
        ]])

(defn current-wallet [app-state events w-name]
  (let [w (get-in app-state [:wallets w-name])]
    [:div
     [:h2 (str "Wallet: " w-name)]
     [:p (apply str (take 30 (:address w)))]
     [:p (str "Balance:" (or (:balance w) "Unknown"))]
     ]))

(defn wallet-list [app-state events]
  [:div
   (when-let [w-name (:current-wallet app-state)]
     (current-wallet app-state events w-name))
   [:h3 "Local Wallets"]
   [:ul (map (partial wallet-li app-state events)
             (vals (:wallets app-state)))]])

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
  (println "render wallets...")
  (case (:wallet-status app-state)
     :not-loaded [:h3 "Loading Wallet..."]
     :empty (prompt-for-wallet app-state events)
     :generating [:h3 "Generating Wallet..."]
     :ready (wallet-list app-state events)
     [:p "Error loading wallet..."]))

(defn node-list [app-state events]
  [:div
    [:h2 "Connected Nodes"]
    [:ul (map (fn [n] [:li (str n)]) (:nodes app-state))]])

(defn main [app-state events]
  [:div
   (wallets app-state events)
   (node-list app-state events)])
