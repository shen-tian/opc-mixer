(ns opcmixer.mixer
  (require
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [clojure.edn :as edn]
   [aleph.tcp :as tcp]
   [gloss.core :as gloss]
   [gloss.io :as io])
  (:gen-class))

;; The OPC protocol. Thanks gloss!
(def opc-protocol
  (gloss/compile-frame
   [:ubyte 
    :ubyte
    (gloss/repeated :ubyte :prefix :uint16)]))


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

(defn my-handler
  [s info]
  (prn (str (:remote-addr info)))
  (s/consume #(prn (count (nth % 2))) s))


(defn -main
  "I don't do a whole lot."
  [& args]
  (def s (start-server my-handler 7890))
  (prn "Server up. Listening...")
  (read-line))
