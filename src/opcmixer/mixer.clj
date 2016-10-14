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

(defn opc-client
  [host port]
  (d/chain (tcp/client {:host host, :port port})
           #(wrap-duplex-stream opc-protocol %)))

(def x (atom []))

(def i-ch (atom {}))

(defn print-handler
  [opc-struct]
  (let [len (count (nth opc-struct 2))]  
    (if (= len 5)
      (prn "special: " (str opc-struct))
      (swap! i-ch (fn [i] {:timestamp (System/currentTimeMillis)
                           :colors (take 10 (nth opc-struct 2))})))
    ;;(prn (System/currentTimeMillis))
    (swap! x #(conj % (System/currentTimeMillis)))
    ;;(prn len)
    ))  

(defn my-handler
  "OPC Handler."
  [s info]
  (s/on-drained s #(prn "closed"))
  (prn (str "new conn: "(:remote-addr info)))
  (s/consume print-handler s))


(defn hello-world-handler
  [req]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body (str "hello world!" @i-ch)})

(defn start-web
  []
  (http/start-server hello-world-handler {:port 8080}))

(defn -main
  "Entry point."
  [& args]
  (def s (start-server my-handler 7890))
  ;;(def s2 (http/start-server hello-world-handler {:port 10000}))
  ;;(def ping (s/periodically 500 (fn [] "hi")))
  ;;(s/consume #(prn %) ping)
  (prn "Server up. Listening...")
  (read-line))
