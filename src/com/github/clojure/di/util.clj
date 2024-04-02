(ns com.github.clojure.di.util)

(defn collect
  "Collect the values of (fn item) from the col where result is not nil"
  [fn col]
  (filter #(not (nil? %)) (map fn col)))

(defn true-metas
  "get the keys of metadata of the object where it's value is true"
  [obj]
  (collect (fn [[k v]] (when (true? v) k)) (meta obj)))

(defn has-meta
  ([k obj] (has-meta k true obj))
  ([k v obj]
   (= (k (meta obj)) v)))

(defn keep-meta
  "return the result of (fun obj), and asign metadata of obj to the result"
  [obj fun & args]
  (if-let [m (meta obj)]
    (with-meta (apply fun obj args) m)
    (apply fun obj args)))


(comment
  ;; 
  )