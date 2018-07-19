(ns clj-nats-streaming.core
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log])
  (:import [java.util.concurrent TimeUnit]
           [io.nats.streaming MessageHandler StreamingConnectionFactory])
  (:gen-class))


;https://en.wikibooks.org/wiki/Clojure_Programming/Examples#Invoking_Java_method_through_method_name_as_a_String
(defn str-invoke [instance method-str & args]
  (clojure.lang.Reflector/invokeInstanceMethod 
      instance 
      method-str 
      (to-array args)))

;should create connection with options
(defn createConnection
 [clusterName clientName options]
 (let [cf (StreamingConnectionFactory. clusterName clientName)
       opts (new io.nats.streaming.Options$Builder)] ; not used for now
  ; https://stackoverflow.com/questions/6685916/how-to-iterate-over-map-keys-and-values
  (doseq [[k v] options] 
    (cond 
      (= k :serverUrl) (.setNatsUrl cf v))) 
  (.createConnection cf)))


(defrecord Connection [clusterName clientName options conn]
  component/Lifecycle
  (start [c]
    (if-not conn (assoc c :conn (createConnection clusterName clientName options))
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


"creates instance of MessageHandler with custom onMessage method"
(defn new-message-handler
  [fns]
  (proxy [MessageHandler] []
    (onMessage [#^io.nats.streaming.Message msg] (doseq [f fns] (f msg)))))

    



(defn subscribe
  "Subscribe to `subject` with MessageHandler instances constructed
  from `fns`, a sequence of functions taking one argument. Returns a
  subscription object that can be closed with `unsubscribe`. `options`
  hashmap may contain key :subscriptionOptions with value as array of 
  methodname and arguments. example: {:subscriptionOptions [\"startAtSequence\",22]}
  Possible methods: startWithLastReceived, deliverAllAvailable, startAtSequence, 
  startAtTime, startAtTimeDelta. See: https://github.com/nats-io/java-nats-streaming"
  [connection subject fns options]
; subscribe(subject, new MessageHandler() {}, SubscriptionOptions)
  (let [opts (new io.nats.streaming.SubscriptionOptions$Builder)]    
    (.subscribe connection
      subject
      (new-message-handler fns)
      (if-let [list (:subscriptionOptions options)] 
       (.build (apply str-invoke opts (first list) (rest list)))
       (.build opts)))))


(defn unsubscribe
  "Close `subscription`, an object returned from `subscribe`. Returns
  nil."
  [subscription]
(.close subscription))


(defrecord Subscription [connection topic fns options subscription]
  component/Lifecycle
  (start [c]
    (if-not subscription
      (assoc c :subscription (subscribe (:conn connection) topic fns options))
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
  [topic fns options]
  (map->Subscription {:topic topic :fns fns :options options}))


