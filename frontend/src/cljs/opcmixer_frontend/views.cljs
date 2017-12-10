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
  (let [ch (re-frame/subscribe [:chan-info name])
        frame (re-frame/subscribe [:channel name])]
    (fn [name]
      [:div
       [:p (:name @ch)]
       [:input {:type "checkbox"
                :checked (:show @ch)
                :on-change #(toggle-show name)}]
       [:input {:type "range"
                :value (:level @ch)
                :on-change #(update-level name
                                          (-> % .-target .-value))}]
       [:button {:on-click #(re-frame/dispatch [:fetch-frame name])}
        "fetch"]
       (into
        [:svg {:width 512
               :height 8}]
        (for [i (range 64)]
          (let [[red green blue] (take 3 (drop (* 3 i) (:frame @frame)))]
            [:rect {:x (* 8 i)
                    :y 0
                    :width 8
                    :height 8
                    :style {:fill @(col/as-css (col/rgba (/ red 255)
                                                         (/ green 255)
                                                         (/ blue 255) 1))}}]))

         )])))

(defn main-panel []
  (let [ch-names (re-frame/subscribe [:ch-names])]
    (fn []
      [:div
       [:h1 "OPC Mixer"]
       [:button {:type "button"
                 :on-click #(re-frame/dispatch [:get-frame])} "Fetch"]
       [:button {:type "button"
                 :on-click #(re-frame/dispatch [:post-frame])} "Send"]
       [:button {:type "button"
                 :on-click #(re-frame/dispatch [:open-ws])} "Open"]
       [:button {:type "button"
                 :on-click #(re-frame/dispatch
                             [:send-msg (js/Date.now)])} "Send"]
       [:div @(re-frame/subscribe [:last-message])]
       (for [ch @ch-names]
         ^{:key ch}
         [channel-comp ch])
       [log-panel]])))
