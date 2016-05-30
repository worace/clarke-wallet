(ns clarke-wallet.views
  (:require [cljs.core.async :as a]
            [reagent.core :as r]))

(def clipboard (js/require "clipboard"))
(defn copy-text [string] (.writeText clipboard string))

(defn wallet-li [app-state events wallet]
  ^{:key wallet}
  [:li
   [:p
    [:b
     {:on-click #(a/put! events {:event :wallet-selected
                                 :data wallet})
      :style {:text-decoration "underline"}}
     (:name wallet)]
    ]])

(defn current-wallet [app-state events w-name]
  (let [w (get-in app-state [:wallets w-name])]
    [:div
     [:h2 (str "Wallet: " w-name)]
     [:p (apply str (take-last 40 (:address w)))]
     [:p {:on-click #(copy-text (:address w))
          :style {:text-decoration "underline"}}
      "Copy Full Address"]
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
  (case (:wallet-status app-state)
     :not-loaded [:h3 "Loading Wallet..."]
     :empty (prompt-for-wallet app-state events)
     :generating [:h3 "Generating Wallet..."]
     :ready (wallet-list app-state events)
     [:p "Error loading wallet..."]))

(defn node-list [app-state events]
  [:div
    [:h2 "Connected Nodes"]
    [:ul (map (fn [n] ^{:key n} [:li (str n)]) (:nodes app-state))]])

(defn address-li [app-state events addr]
  ^{:key addr}
  [:li
   [:p (str "Name: " (:name addr))]
   [:p (str "Address: " (apply str (take-last 40 (:address addr))))]
   [:p {:on-click #(copy-text (:address addr))
        :style {:text-decoration "underline"}}
    "Copy Full Address"]
   [:input {:type "button" :value "Delete"
            :on-click #(a/put! events {:event :delete-address
                                       :data {:name (:name addr)}})}]
   ]
  )

(defn new-address-form [app-state events]
  (let [addr-name (r/atom "")
        addr-der (r/atom "")]
    [:div
     [:p "Add a new saved address:"]
     [:p
      [:label "Name:"]
      [:input {:type "text"
               :on-change #(reset! addr-name (-> % .-target .-value))}]]
     [:p
      [:label "Address:"]
      [:textarea {:type "text-area"
               :on-change #(reset! addr-der (-> % .-target .-value))}]]
     [:input {:type "button" :value "Save Address"
              :on-click (fn [e]
                          (a/put! events {:event :store-address
                                          :data {:name @addr-name
                                                 :address @addr-der}})
                          (reset! addr-name "")
                          (reset! addr-der ""))}]]))

(defn address-book [app-state events]
  (println "addr book" (:address-book app-state))
  [:div
   [:h2 "Saved Addresses"]
   (into [:ul] (map (partial address-li app-state events)
                    (vals (:address-book app-state))))
   [:hr]
   (new-address-form app-state events)])

(defn main [app-state events]
  [:div
   (wallets app-state events)
   [:hr]
   (node-list app-state events)
   [:hr]
   (address-book app-state events)])
