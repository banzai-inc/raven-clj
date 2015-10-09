(ns raven-clj.ring
  (:require [raven-clj.core :refer [capture]]
            [raven-clj.interfaces :refer [http stacktrace]]))

(defn capture-error [dsn req ^Throwable error extra app-namespaces http-alter-fn]
  (future (capture dsn (-> (merge extra
                                  {:message (.getMessage error)})
                           (http req http-alter-fn)
                           (stacktrace error app-namespaces)))))

(defn- format-error [dsn req e {:keys [extra namespaces http-alter-fn test?]
                                :or {http-alter-fn identity}}]
  (when (not test?)
    (capture-error dsn req e extra namespaces http-alter-fn))
  (throw e))

(defn wrap-sentry [handler dsn & [opts]]
  (fn [req]
    (try
      (handler req)
      (catch Exception e (format-error dsn req e opts))
      (catch AssertionError e (format-error dsn req e opts)))))
