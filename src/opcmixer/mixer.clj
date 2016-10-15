(ns opcmixer.mixer
  (require
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [clojure.edn :as edn]
   [aleph.tcp :as tcp]
   [aleph.http :as http]
   [gloss.core :as gloss]
   [gloss.io :as io]
   [compojure.core :as compojure :refer [GET]]
   [compojure.route :as route])
  (:gen-class))

;; The OPC protocol. Thanks gloss!
(def opc-protocol
  (gloss/compile-frame
   [:ubyte 
    :ubyte
    (gloss/repeated :ubyte :prefix :uint16)]))

;; Not sure what this does...
(defn wrap-duplex-stream
  [protocol s]
  (let [out (s/stream)]
    (s/connect
     (s/map #(io/encode protocol %) out)
      s)
    (s/splice
     out
      (io/decode-stream s protocol))))

(defn start-server
  [handler port]
  (tcp/start-server
    (fn [s info]
      (handler (wrap-duplex-stream opc-protocol s) info))
    {:port port}))

(defn opc-client
  [host port]
  (d/chain (tcp/client {:host host, :port port})
           #(wrap-duplex-stream opc-protocol %)))


(defn add-stream [id]
  (fn [m]
    (assoc m (keyword id) '{:name "name"})))

(defn update-frame [id frame]
  (fn [m]
    (let [key (keyword id)]
      (update m key
              (fn [stream] (assoc stream :frame frame))))))

(defn remove-stream [id]
  (fn [m]
    (dissoc m (keyword id))))

(def opc-streams (atom '{})) ;;   

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

(defn hello-world-handler
  [req]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body (str @opc-streams)})

(compojure/defroutes app-routes
  (GET "/" [] hello-world-handler)
  (GET "/color/" [] (str @i-ch)))

(defn start-web
  []
  (http/start-server hello-world-handler {:port 8080}))

(defn -main
  "Entry point."
  [& args]
  (def s (start-server handler 7890))
  ;;(def s2 (http/start-server hello-world-handler {:port 10000}))
  ;;(def ping (s/periodically 500 (fn [] "hi")))
  ;;(s/consume #(prn %) ping)
  (prn "Server up. Listening...")
  (read-line))
