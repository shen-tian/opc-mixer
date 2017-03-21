(ns opcmixer.mixer
  (:require [aleph.http :as http]
            [aleph.tcp :as tcp]
            [clj-opc.core :as opc]
            [compojure.core :as compojure :refer [GET]]
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

(defn start-server [handler port]
  "Starts the TCP server"
  (tcp/start-server
    (fn [s info]
      (handler (wrap-duplex-stream opc/opc-protocol s) info))
    {:port port}))

(defn get-frame [m]
  "Gets a frame. Pretty messy at the moment, but works"
  (let [stream-count (count m)]
    (if (= stream-count 0)
      [0 0 [0 0 0]]
      [0 0 
       (let [x (into '[] (map #(int (/ % stream-count)) 
                               (reduce (fn [f1 f2] 
                                         (if (= (count f1) (count f2))
                                           (into '[] (map + f1 f2))
                                           f1))
                                       (map #(:frame (last %)) m))))]
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
    (assoc m (keyword id) '{:name "name"})))

(defn remove-stream [id]
  (fn [m]
    (dissoc m (keyword id))))

(defn update-frame [id frame]
  (fn [m]
    (let [key (keyword id)]
      (update m key
              (fn [stream] (assoc stream :frame frame))))))

;; Stores state of input streams
(def opc-streams (atom '{}))   

(defn handler [s info]
  "OPC Handler."
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

(defn hello-world-handler
  [req]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body @opc-streams})


(compojure/defroutes app-routes
  (GET "/" [] hello-world-handler))

(def app 
  (-> app-routes
      wrap-json-body
      wrap-json-response
      (wrap-cors :access-control-allow-origin 
                 [#"http://localhost:3449"]
                 :access-control-allow-methods [:get]
                 :access-control-allow-credentials "true")))

(defn start-web
  []
  (http/start-server app {:port 8080}))

(defn -main [& args]
  "Entry point."
  (def s (start-server handler 7890))
  (def w (start-web))
  (start-client "localhost" 7891 opc-streams)
  (prn "Server up. Listening...")
  (read-line))
