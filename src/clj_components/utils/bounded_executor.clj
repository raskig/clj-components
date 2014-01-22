(ns clj-components.utils.bounded-executor
  "See: https://github.com/aphyr/riemann-clojure-client/issues/9#issuecomment-32624706 to understand what's going on here"
  (:import (java.util.concurrent ThreadPoolExecutor TimeUnit LinkedBlockingQueue RejectedExecutionHandler)))

(def reject-handler
  "Handles a rejection on the bounded executor. i.e. when the LBQ is full."
  (proxy [RejectedExecutionHandler] []
    (rejectedExecution [runnable executor])))

(def bounded-executor
  "Bounded Execution, current settings are calcuated thinking on the current volumes of Riemann In Production"
  (let [cores (.availableProcessors (Runtime/getRuntime))]
    (ThreadPoolExecutor. 1 cores 5 TimeUnit/SECONDS (LinkedBlockingQueue. 250) reject-handler)))

(defn run-bounded [f]
  "Exectutes f in a bounded executor"
  (let [executor bounded-executor]
    (.execute executor (Thread. f))))
