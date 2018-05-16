(ns onyx-too-many-poll-invocations.jobs.basic
  (:require [onyx.job :refer [add-task register-job]]
            [onyx.tasks.core-async :as core-async-task]
            [onyx.plugin.protocols :as p]
            [onyx-too-many-poll-invocations.tasks.math :as math]))

(defrecord TablePartitioner [db-conn completed? page]
  p/Plugin
  (start [this event]
    this)

  (stop [this event]
    this)

  p/BarrierSynchronization
  (synced? [this epoch]
    true)

  (completed? [this]
    (prn "input task completed?" @completed?)
    @completed?)

  p/Checkpointed
  (checkpoint [this]
    @page)

  (recover! [this replica-version checkpoint]
    (prn "recover!")
    (reset! completed? false)
    (if (nil? checkpoint)
      (reset! page 0)
      (reset! page checkpoint)))
  
  (checkpointed! [this epoch])

  p/Input
  (poll! [this segment _timeout-ms]
    ;; TODO: why is it invoked so many times after it is marked as completed?
    (if (> @page 5)
      (do
        (prn "poll! returning nil because we're past the last page")
        (reset! completed? true)
        nil)
      (do
        (prn "poll! returning segments")
        (swap! page inc)
        [{:n 1}]))))

(defn partition-table [{:keys [onyx.core/task-map] :as event}]
  (map->TablePartitioner
    {:page (atom 0)
     :completed? (atom false)}))

(def in
  {:onyx/name             :in
   :onyx/plugin           ::partition-table
   :onyx/type             :input
   :onyx/medium           :sql
   :onyx/batch-size       1000
   :onyx/n-peers          1})

(defn mk-basic-job
  [batch-settings]
  (let [base-job {:workflow [[:in :inc]
                             [:inc :out]]
                  :catalog [in]
                  :lifecycles []
                  :windows []
                  :triggers []
                  :flow-conditions []
                  :task-scheduler :onyx.task-scheduler/balanced}]
    (-> base-job
        (add-task (math/inc-key :inc [:n] batch-settings))
        (add-task (core-async-task/output :out batch-settings)))))

(def basic-job
  (let [batch-settings {:onyx/batch-size 1 :onyx/batch-timeout 1000}]
    (mk-basic-job batch-settings)))
