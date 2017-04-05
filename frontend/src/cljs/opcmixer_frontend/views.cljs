(ns opcmixer-frontend.views
    (:require [re-frame.core :as re-frame]
              [thi.ng.color.core :as col]))

(defn log-panel
  []
  (let [log @(re-frame/subscribe [:app-log])]
    [:div
     [:p (str log)]]))

(defn update-level
  [ch-name new-val]
  (re-frame/dispatch [:change-level (keyword ch-name) new-val]))

(defn toggle-show
  [ch-name]
  (re-frame/dispatch [:toggle-show (keyword ch-name)]))

(defn channel-comp
  [name]
  (let [ch (re-frame/subscribe [:chan-info name])]
    (fn [name]
      [:div
       [:p (:name @ch)]
       [:input {:type "checkbox"
                :checked (:show @ch)
                :on-change #(toggle-show name)}]
       [:input {:type "range"
                :value (:level @ch)
                :on-change #(update-level name (-> % .-target .-value))}]])))

(defn main-panel []
  (let [ch-names (re-frame/subscribe [:ch-names])]
    (fn []
      [:div 
       [:h1 "OPC Mixer"]
       [:button {:type "button"
                 :on-click #(re-frame/dispatch [:get-frame])} "Fetch"]
       [:button {:type "button"
                 :on-click #(re-frame/dispatch [:post-frame])} "Send"]
       (for [ch @ch-names]
         ^{:key ch}
         [channel-comp ch])
       [log-panel]])))
