(ns opcmixer-frontend.events
  (:require [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [opcmixer-frontend.db :as db]
            [chord.client :refer [ws-ch]]
            ;;[cljs.core.async :refer [<!!]]
)
  (:require-macros [cljs.core.async.macros :refer [go]]))

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   db/default-db))


(re-frame/reg-event-db
 :process-blah-response
 (fn [db [_ x]]
   (assoc db :app-log x)))

(re-frame/reg-event-db
 :print-response
 (fn [db [_ x]]
   (prn x)
   db))

(re-frame/reg-event-db
 :change-level
 (fn [db [_ channel new-level]]
   (assoc-in db [:app-log channel :level] new-level)))

(re-frame/reg-event-db
 :toggle-show
 (fn [db [_ channel]]
   (update-in db [:app-log channel :show] not)))

(re-frame/reg-event-db
 :frame-changed
 (fn [db [_ id new-frame]]
   (assoc-in db [:channel id :frame] new-frame)))

(re-frame/reg-event-fx
 :fetch-frame
 (fn [_ [_ id]]
   {:http-xhrio {:method :get
                 :uri (str "http://localhost:8080/channel/" (name id))
                 :response-format (ajax/json-response-format
                                   {:keywords? true})
                 :on-success [:frame-changed (name id)]
                 :on-failure [:print-response id]}}))

(re-frame/reg-event-fx
 :get-frame
 (fn [{:keys [db]} [_ a]]
   {:http-xhrio {:method :get
                 :uri "http://localhost:8080/controls/"
                 :response-format (ajax/json-response-format
                                   {:keywords? true})
                 :on-success  [:process-blah-response]
                 :on-failure  [:process-blah-response]}
    :db (assoc db :flag true)}))

(re-frame/reg-event-fx
 :post-frame
 (fn [{:keys [db]} [_ a]]
   {:http-xhrio {:method :post
                 :uri "http://localhost:8080/controls/"
                 :params @(re-frame/subscribe [:app-log])
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format
                                   {:keywords? true})
                 :on-success  [:print-response]
                 :on-failure  [:print-response]}
    :db (assoc db :flag false)}))

(defonce ws-chan (atom nil))

(re-frame/reg-event-db
 :open-ws
 (fn [db _]
   (go
     (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:8080/echo"))]
       (if-not error
         (do
           (reset! ws-chan ws-channel)
           (while true
             (let [{:keys [message]} (<! ws-channel)]
               (re-frame/dispatch [:recv-msg message]))))

         (js/console.log "Error:" (pr-str error)))))
   db))

(re-frame/reg-event-db
 :send-msg
 (fn [db [_ msg]]
   (go (>! @ws-chan msg))
   db))

(re-frame/reg-event-db
 :recv-msg
 (fn [db [_ msg]]
   (assoc db :message msg)))
