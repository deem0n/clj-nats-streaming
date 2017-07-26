(ns clj-nats-streaming.core
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log])
  (:import [java.util.concurrent TimeUnit]
           [io.nats.stan MessageHandler ConnectionFactory])
  (:gen-class))


(defrecord Connection [clusterName clientName options conn]
  component/Lifecycle
  (start [c]
    (if-not conn
      (let [cf (ConnectionFactory. clusterName clientName)]
        (assoc c :conn (.createConnection cf)))
      c))
  (stop [c]
    (try
      (if conn
        (do (.close conn)
            (assoc c :conn nil))
        c)
      (catch Throwable t
        (log/warn "Connection encountered error while stopping.")
c))))


(defn connection
  "Component constructor for NATS Streaming connection."
  [clusterName clientName options]
(map->Connection {:clusterName clusterName :clientName clientName :options options}))



(defn new-message-handler
  [f]
  (proxy [MessageHandler] []
    (onMessage [#^io.nats.stan.Message msg] (f msg))))


(defn subscribeM
  "Subscribe to `subject` with MessageHandler instances constructed
  from `fns`, a sequence of functions taking one argument. Returns a
  subscription object that can be closed with `unsubscribe`."
  [connection subject & fns]
  (->> fns
       (map new-message-handler)
       (into-array MessageHandler)
       (.subscribe (:conn connection) subject)))

(defn subscribe
  "Simple version"
  [connection subject & f]
  (.subscribe (:conn connection)
              subject
              (new-message-handler (first f))
              (-> (new io.nats.stan.SubscriptionOptions$Builder) (.deliverAllAvailable) (.build))
              ))


(defn unsubscribe
  "Close `subscription`, an object returned from `subscribe`. Returns
  nil."
  [subscription]
(.close subscription))


(defrecord Subscription [connection topic fns subscription]
  component/Lifecycle
  (start [c]
    (if-not subscription
      (assoc c :subscription (apply subscribe connection topic fns))
      c))
  (stop [c]
    (try
      (if subscription
        (do (unsubscribe subscription)
            (assoc c :subscription nil))
        c)
      (catch Throwable t
        (log/warn "Subscription encountered error while stopping.")
        c))))

(defn subscription
  "Subscription component constructor. Each element of `fns` is
  evaluated with a single `Message` argument for each matching
  message."
  [topic fns]
(map->Subscription {:topic topic :fns fns}))


