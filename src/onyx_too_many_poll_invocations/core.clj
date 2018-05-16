(ns onyx-too-many-poll-invocations.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [onyx.api :as onyx.api]
            [onyx-too-many-poll-invocations.jobs.basic :as job]))

(defn read-config [resource-path]
  (-> resource-path io/resource slurp edn/read-string))

(def env-config (read-config "env_config.edn"))
(def peer-config (read-config "peer_config.edn"))

(defn start-env
  "Starts an in-memory ZooKeeper instance, among other things."
  []
  (onyx.api/start-env env-config))

(defn start-peers [n-of-peers]
  (let [peer-group (onyx.api/start-peer-group peer-config)
        peers (onyx.api/start-peers n-of-peers peer-group)]
    peers))

(defn process-job
  "Submits job and blocks until it's completed."
  [job]
  (let [{:keys [job-id success?]} (onyx.api/submit-job peer-config job)]
    (if success?
      (onyx.api/await-job-completion peer-config job-id)
      false)))

(defn -main [& args]
  (start-env)
  (start-peers 20)
  (process-job job/basic-job))
