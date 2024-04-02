;; private functions for internal usage

(ns com.github.clojure.di.impl
  (:require [com.github.clojure.di.util :as util]
            [com.github.clojure.di.core :as-alias di]
            [clojure.string :as str]))


(defn- build-opts [symbol-map opt-map]
  (letfn [;;transfrom {opt-key value} to {opt-key {symbol-name value}}
          (tranform [k opts]
            (update-vals opts (fn [a] {k a})))]
    (reduce (fn [acc sym]
              (if-let [metas (util/true-metas sym)]
                (->> metas
                     (map opt-map)
                     (reduce merge {})
                     (tranform (symbol-map sym))
                     (merge-with merge acc))
                acc))
            {}
            (keys symbol-map))))

(defn parse-args [destruce-map opt-map]
  (when (map? destruce-map)
    (let [sym-map (reduce (fn [acc [k v]]
                            (cond
                              (= :keys k) (into acc (map (fn [sym] [sym (-> sym name keyword)]) v))
                              (and (qualified-keyword? k)
                                   (= (name k) "keys")) (let [ns (namespace k)]
                                                          (into acc (map (fn [sym] [sym (->> sym name (keyword ns))]) v)))
                              (symbol? k) (assoc acc k v)
                               ;; (map? k) (parse-deps k)
                              :else acc))
                          {}
                          destruce-map)
          res {::di/in (-> sym-map vals set)}
          optional-sym (:or destruce-map)
          res (if (seq optional-sym)
                (assoc res ::di/optional (set (map sym-map (keys optional-sym))))
                res)
          res  (let [opts (build-opts sym-map opt-map)]
                 (if (seq opts)
                   (assoc res ::di/opts opts)
                   res))]
      res)))

(defn parse-resp [body]
  (let [resp (last body)]
    (cond
      (map? resp) {::di/out (set (keys resp))}
      (seq? resp) (parse-resp resp)
      :else (throw (new IllegalArgumentException
                        "A component must return a map")))))

(defn- calc-deps
  "calculate the dependencies of each component
  return list of [comp-name #{depdencies names}] for each component"
  [components]
  (let [;; translate the map for {name -> out [p1 p2]} to {p1 -> (name1 name2), p2-> (name)}
        provide-map  (reduce (fn [acc [k n]] (update acc k conj n))
                             {}
                             (mapcat (fn [{::di/keys [out name]}]
                                       (map #(list % name) out))
                                     components))]
    (map (fn [{::di/keys [name in]}]
           [name  (->> in
                       (map provide-map)
                       (filter #(not (nil? %)))
                       (mapcat identity)
                       (set))])
         components)))

(defn init-order
  "return the list of components, sorted by the init order"
  [components]
  (let [deps (calc-deps components)
        sorted (sort (fn [[na dpa] [nb dpb]]
                       (cond
                         (dpb na) -1
                         (dpa nb) 1
                         :else (compare na nb)))
                     deps)]
    (map first sorted)))


(defn merge-ctx [ctx new merge-fns]
  (reduce (fn [acc [k v]]
            (let [current-v (get ctx k)]
              (if-let [merge-fn (get merge-fns k)]
                (assoc acc k (merge-fn k current-v v))
                (assoc acc k (cond
                               (nil? current-v) v
                               (and
                                (vector? current-v)
                                (util/has-meta ::di/merged-vector current-v)) (util/keep-meta current-v conj v)
                               :else (util/keep-meta ^::di/merged-vector [] conj current-v v))))))
          ctx
          new))

(defn check-deps [{::di/keys [name optional in]
                   :or {optional #{}}} 
                  context]
  (when-let [not-exist (seq (filter #(and
                                      (not (optional %))
                                      (not (contains? context %)))
                                    in))]
    (throw  (ex-info (str "[" name "] missing required param [" (str/join not-exist) "]") {:comp name
                                      :missing (vec not-exist)}))))