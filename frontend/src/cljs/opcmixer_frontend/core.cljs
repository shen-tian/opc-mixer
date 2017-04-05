(ns opcmixer-frontend.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [opcmixer-frontend.events]
              [opcmixer-frontend.subs]
              [opcmixer-frontend.views :as views]
              [opcmixer-frontend.config :as config]))

#_(defonce do-timer (js/setInterval #(re-frame/dispatch [:get-frame]) 100))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
