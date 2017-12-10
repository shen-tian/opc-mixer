(ns opcmixer-frontend.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 :app-log
 (fn [db _]
   (:app-log db)))

(re-frame/reg-sub
 :color
 (fn [db _]
   (:hue db)))

(re-frame/reg-sub
 :channel
 (fn [db [_ id]]
   (get-in db [:channel (name id) :frame])))

(re-frame/reg-sub
 :pix
 (fn [db _]
   (:pix db)))

(re-frame/reg-sub
 :ch-names
 (fn [db _]
   (keys (:app-log db))))

(re-frame/reg-sub
 :chan-info
 (fn [db [_ name]]
   (get-in db [:app-log (keyword name)])))

(re-frame/reg-sub
 :last-message
 (fn [db _]
   (:message db)))
