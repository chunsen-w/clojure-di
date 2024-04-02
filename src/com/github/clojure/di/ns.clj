(ns com.github.clojure.di.ns
  (:require [clojure.string :as str]
            [clojure.java.classpath :as cp])
  (:import (java.io File)))


(def ^:private ns-fine-name-sufixes ["__init.class" ".clj" ".cljc"])

(defn- to-file [f]
  (cond
    (instance? File f) f
    :else (File. (str f))))

(defn- lst-files [^File f]
  (cond
    (.isFile f) (list f)
    (.isDirectory f) (let [files (.listFiles f)
                           [is-files dirs] (split-with #(.isFile %) files)]
                       (concat is-files (mapcat lst-files dirs)))
    :else (list)))

(defn- scan-dirs [dirs]
  (mapcat
   (fn [dir]
     (let [dir-file (to-file dir)
           path-prefix-length (-> dir-file (.getCanonicalPath) count inc)
           files (lst-files dir-file)]
       (map #(subs (.getCanonicalPath %) path-prefix-length) files)))
   dirs))


(defn- file-name-to-ns [f-name]
  (when-let [trimmed  (some
                       (fn [sufix]
                         (when (str/ends-with? f-name sufix)
                           (subs f-name 0 (- (count f-name)
                                             (count sufix)))))
                       ns-fine-name-sufixes)]
    (->> (str/split trimmed #"/")
         (map #(str/replace % #"_" "-"))
         (str/join "."))))


(defn scan-ns
  "Rerurn a string set, for all the namespaces which have the prefix"
  [prefix]
  (let [name-prefix (->> (str/split prefix #"\.")
                         (map (fn [seg]
                                (str/replace seg #"-" "_")))
                         (str/join "/"))
        name-fitler (fn [name]
                      (and
                       (str/starts-with? name name-prefix)
                       (some #(str/ends-with? name %) ns-fine-name-sufixes)))
        file-names (concat
                    (mapcat cp/filenames-in-jar (cp/classpath-jarfiles))
                    (scan-dirs (cp/classpath-directories)))
        namespaces (map file-name-to-ns (filter name-fitler file-names))]
    (set namespaces)))



