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
             (reset! wallet-der data)
             (-> (doto (node-rsa) (.importKey @wallet-der "pkcs8"))
                 (.sign (js/Buffer. "pizza") "base64"))))

;; (def keypair
;;   (doto (node-rsa) (.importKey @wallet-der "pkcs8")))

;; (.sign keypair (js/Buffer. "pizza") "base64")

;; (.sign (doto (node-rsa)
;;          (.importKey @wallet-der "pkcs8")) (js/Buffer. "pizza") "base64")

;; Booting
;; Read files in wallet dir
;; if none, prompt to create
;; connect to DNSServer to find available nodes
;; fetch balance for wallet addr


;; function getTestPersonaLoginCredentials(callback) {

;;     return http.get({
;;         host: 'personatestuser.org',
;;         path: '/email'
;;     }, function(response) {
;;         // Continuously update stream with data
;;         var body = '';
;;         response.on('data', function(d) {
;;             body += d;
;;         });
;;         response.on('end', function() {

;;             // Data reception is done, do whatever with it!
;;             var parsed = JSON.parse(body);
;;             callback({
;;                 email: parsed.email,
;;                 password: parsed.pass
;;             });
;;         });
;;     });

;; },

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

(http-get "dns1.clarkecoin.org" "/api/peers" println)
