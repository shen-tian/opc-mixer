(ns opcmixer.mixer
  (require
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [clojure.edn :as edn]
   [aleph.tcp :as tcp]
   [aleph.http :as http]
   [gloss.core :as gloss]
   [gloss.io :as io])
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

(def x (atom 0))

(defn print-handler
  [opc-struct]
  (let [len (count (nth opc-struct 2))]  
    (swap! x (fn [n] len))
    ;;(prn len)
    ))

(defn my-handler
  "OPC Handler."
  [s info]
  (prn (str (:remote-addr info)))
  (s/consume print-handler s))


(defn hello-world-handler
  [req]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body (str "hello world!" @x)})

(defn -main
  "Entry point."
  [& args]
  (def s (start-server my-handler 7890))
  (def s2 (http/start-server hello-world-handler {:port 10000}))
  (prn "Server up. Listening...")
  (read-line))
