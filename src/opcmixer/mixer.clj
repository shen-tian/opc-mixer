(ns opcmixer.mixer
  (:require [aleph.http :as http]
            [aleph.tcp :as tcp]
            [clj-opc.core :as opc]
            [compojure.core :as compojure :refer [GET POST]]
            [compojure.route :as route]
            [gloss.io :as io]
            [manifold.stream :as s]
            [ring.middleware.json :refer [wrap-json-params 
                                          wrap-json-response 
                                          wrap-json-body]]
            [ring.middleware.cors :refer [wrap-cors]])
  (:gen-class))

(defn wrap-duplex-stream
  [protocol s]
  "Generally useful function. Create the return stream out.
  Then it attaches the encoder and decoders, and splies the return
  stream with the incoming stream."
  (let [out (s/stream)]
    (s/connect
     (s/map #(io/encode protocol %) out)
     s)
    (s/splice
     out
      (io/decode-stream s protocol))))

(defn start-tcp [handler port]
  "Starts the TCP server"
  (tcp/start-server
    (fn [s info]
      (handler (wrap-duplex-stream opc/opc-protocol s) info))
    {:port port}))

(defn get-frame-blend
  [stream-count m]
  (map #(int (* (/ (:level (val (first m))) 100) 
                (/ % stream-count))) 
       (reduce (fn [f1 f2] 
                 (if (= (count f1) (count f2))
                   (into '[] (map + f1 f2))
                   f1))
               (map #(:frame (val %)) m))))

(defn get-frame [m]
  "Gets a frame. Pretty messy at the moment, but works"
  (let [stream-count (count m)]
    (if (= stream-count 0)
      [0 0 [0 0 0]]
      [0 0 
       (let [x (into '[] (get-frame-blend stream-count m))]
         (if (<  (count x) 1) 
           [0 0 0]
           x))])))


(defn start-client [host port m]
  "This is pretty neat: creates a new stream that uses get-frame
   to generate output, running at 60fps, then connects that to the
   opc-client sink directly"
  (s/connect
   (s/periodically 17 #(get-frame @m))
   (:in @(opc/client host port))))

(defn add-stream [id]
  (fn [m]
    (assoc m (keyword id) '{:name "name"
                            :show true
                            :level 100})))

(defn remove-stream [id]
  (fn [m]
    (dissoc m (keyword id))))

(defn update-frame [id frame]
  (fn [m]
    (let [key (keyword id)]
      (update m key
              (fn [stream] (assoc stream :frame frame))))))

;; Stores state of input streams
(defonce opc-streams (atom '{}))   

(defn handler
  "OPC Handler."
  [s info]
  (let [id (str (System/currentTimeMillis))]
    (s/on-drained s (fn [] 
                      (prn "closed")
                      (swap! opc-streams (remove-stream id))))
    (prn (str "new conn: "(:remote-addr info)))
    (swap! opc-streams (add-stream id))
    (s/consume (fn [opc-struct]
                 (let [frame (nth opc-struct 2)]
                   (if (= (count frame) 5)
                     (prn "special: " (str frame))
                     (swap! opc-streams (update-frame id frame))))) s)))

;;;;;;;;;; The web parts ;;;;;;;;;;;;;

(defonce foo-ctrl (atom {}))

(defn hello-world-handler
  [req]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body @opc-streams})

(defn map-values
  [m keys f & args]
  (reduce #(apply update-in %1 [%2] f args) m keys))


(defn control-hanlder
  [req]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body (let [streams @opc-streams]
           (map-values streams
                       (keys streams)
                       dissoc :frame))})


(compojure/defroutes app-routes
  (GET "/" [] hello-world-handler)
  (GET "/controls/" [] control-hanlder)
  (POST "/controls/" {body :body} 
        (let [k (keyword (first (keys body)))]
          (prn body)
          (swap! opc-streams 
                 #(assoc-in % 
                            [k :level]
                            (read-string (get-in body [k :level]))))))
  (route/not-found "<h1>Page not found</h1>"))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true :bigdecimals? true})
      wrap-json-response
      (wrap-cors :access-control-allow-origin 
                 [#"http://localhost:3449"]
                 :access-control-allow-methods [:get :post]
                 :access-control-allow-credentials "true")))

(defn start-web
  [handler port]
  (http/start-server handler {:port port}))

;;;;;;;;; App entry ;;;;;;;;;;;;;;;;;;

(defonce app-state (atom '{}))

(defn start-server
  []
  (swap! app-state assoc :s (start-tcp handler 7890))
  (swap! app-state assoc :w (start-web app 8080)))

(defn stop-server
  []
  (.close (:s @app-state))
  (.close (:w @app-state))
  (swap! app-state dissoc :s :w))

(defn -main 
  "Entry point."
  [& args]
  (start-server)
  (start-client "localhost" 7891 opc-streams)
  (prn "Server up. Listening...")
  (read-line))
