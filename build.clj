(ns build
  (:require [clojure.tools.build.api :as b]))


(def build-folder "target")
(def jar-content (str build-folder "/classes"))     ; folder where we collect files to pack in a jar

(def lib-name 'com.github/clj-di)      ; library name
(def version "0.0.1-SNAPSHOT")                               ; library version
(def basis (b/create-basis {:project "deps.edn"}))  ; basis structure (read details in the article)
(def jar-file-name (format "%s/%s-%s.jar" build-folder (name lib-name) version))  ; path for result jar file

(defn clean [_]
  (b/delete {:path build-folder})
  (println (format "Build folder \"%s\" removed" build-folder)))

(defn jar [_]
  (clean nil)                                     ; clean leftovers 

  (b/copy-dir {:src-dirs   ["src" "resources"]    ; prepare jar content
               :target-dir jar-content})

  (b/compile-clj {:basis     basis               ; compile clojure code
                  :src-dirs  ["src"]
                  :class-dir jar-content
                  :filter-nses ['com.github.clojure.di]})

  (b/write-pom {:class-dir jar-content            ; create pom.xml
                :lib       lib-name
                :version   version
                :basis     basis
                :src-dirs  ["src"]})

  (b/jar {:class-dir jar-content                  ; create jar
          :jar-file  jar-file-name})
  (println (format "Jar file created: \"%s\"" jar-file-name)))

(defn publish-local [_]
  (jar nil)
  (b/install {:class-dir jar-content            ; create pom.xml
              :lib       lib-name
              :jar-file  jar-file-name
              :basis     basis
              :version   version}))