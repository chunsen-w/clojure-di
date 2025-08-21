(ns com.github.clojure.di.core
  (:require [com.github.clojure.di.impl :as impl]
            [com.github.clojure.di.util :as util]
            [com.github.clojure.di.graph :as graph]
            [clojure.tools.logging :as log]
            [clojure.set :as set]))

(def ^:private di-name
  (let [index (atom 0)]
    (fn [prefix]
      (keyword (-> *ns* ns-name name) (str prefix "-" (swap! index inc))))))

(defmacro di [& fdecl]
  (let [[maybe-name args] (cond
                            (symbol? (first fdecl)) [(first fdecl) (fnext fdecl)]
                            :else [nil (first fdecl)])
        name (or
              (:clj-di/di-name (meta &form))
              (when (symbol? maybe-name) (di-name (name maybe-name)))
              (di-name "di"))
        _ (assert (keyword? name))
        _ (assert (vector? args))
        _ (assert (<= 0 (count args) 1) "Component should have 0 or 1 argument")
        deps (impl/parse-args (first args))
        provide (impl/parse-resp (next fdecl))
        meta-map (merge
                  {:clj-di/name name
                   :clj-di/no-args? (nil? (seq args))}
                  deps
                  provide)]
    `(with-meta (fn ~@fdecl) '~meta-map)))


(defmacro defdi [fn-name & fdecl]
  (let [comp-name (keyword (-> *ns* ns-name name) (name fn-name))
        m {:clj-di/di true}
        m (if (string? (first fdecl))
            (assoc m :doc (first fdecl))
            m)
        fdecl (if (string? (first fdecl))
                (next fdecl)
                fdecl)]
    `(def ~(with-meta fn-name m)
       ~(with-meta `(di ~@fdecl) {:clj-di/di-name comp-name}))))

(defmacro defcomp
  "a macro to define a di component
   The arguments will injected automatically by di
   And the return value can be injected by other components through the qualified keyword of this funcion"
  [fn-name & fdecl]
  (let [the-key (keyword (-> *ns* ns-name name) (name fn-name))
        [doc arg body] (cond
                         (string? (first fdecl)) [(first fdecl) (fnext fdecl) (nnext fdecl)]
                         :else [nil (first fdecl) (next fdecl)])
        body (concat (drop-last body) [{the-key (last body)}])]
    `(defdi ~fn-name ~@(if doc [doc arg] [arg]) ~@body)))

(defn execute
  "execute the components in their dependency order
   supported options:
   :merge-fn - a map of {key merge-fn}, 
     the merge-fn is a function that takes a key and a list of values, and return the merged value
     where the value is a pair of [key-of-the-provider value]
   :init-roots - a seq of the components key to be inited, if roots is not empty, then only roots and their dependencies will be inited"
  ([components] (execute components {} {}))
  ([components init-ctx] (execute components init-ctx {}))
  ([components init-ctx opts]
   (let [components (map #(let [{:clj-di/keys [name] :as metadata} (meta %)]
                            (when-not name
                              (throw (ex-info (str "illegal component " %) {:comp %})))
                            (assoc metadata :clj-di/fn %))
                         components)
         name-map (util/to-map-by :clj-di/name components)
         provider-map (impl/calc-provider components)
         ;; create a graph by dependency order
         ;; if a depends on b, then make graph edge b -> a
         deps-graph (graph/build-graph (mapcat (fn [{:clj-di/keys [name in]}]
                                                 (or (seq (mapcat (fn [[k _]]
                                                                    (map (fn [p] [p name]) (provider-map k)))
                                                                  in))
                                                     [name]))
                                               components))
         _ (when-let [cycles (seq (graph/detect-cycle deps-graph))]
             (throw (ex-info "cyclic dependency detected" {:cycles cycles})))
         init-order (graph/topological-sort deps-graph)
         init-order (if-let [init-roots (:init-roots opts)]
                      (let [all-keys (-> provider-map keys set)
                            unknown (set/difference (set init-roots) all-keys)]
                        (if (seq unknown)
                          (throw (ex-info "roots optiosn contains unknow keys" {:keys unknown}))
                          (let [reversed-graph (graph/reverse-graph deps-graph)
                                effected-components (mapcat #(graph/dfs-visit reversed-graph %) (mapcat provider-map init-roots))]
                            (filter (set effected-components) init-order))))
                      init-order)
         _ (log/debug "init order:" init-order)]
     (reduce (fn [ctx name]
               (let [comp (get name-map name)
                     input  (impl/select-input comp ctx)
                     res (if (:clj-di/no-args? comp)
                           ((:clj-di/fn comp))
                           ((:clj-di/fn comp) input))]
                 (impl/merge-ctx ctx res name)))
             (impl/merge-ctx {} init-ctx :init)
             init-order))))

(defn ctx-values
  "return the values of the key in the context"
  [ctx key]
  (map second (get ctx key)))