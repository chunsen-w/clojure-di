(ns build
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))


(def build-folder "target")
(def jar-content (str build-folder "/classes"))     ; folder where we collect files to pack in a jar

(def lib-name 'com.github.chunsen-w/clj-di)           ; library name
(def basis (b/create-basis {:project "deps.edn"}))  ; basis structure (read details in the article)

; library version
(defn version [opt]
  (or (:version opt) "0.0.1-SNAPSHOT"))
; path for result jar file
(defn jar-file-name [opt]
  (format "%s/%s-%s.jar" build-folder (name lib-name) (version opt)))

(defn clean [_]
  (b/delete {:path build-folder})
  (println (format "Build folder \"%s\" removed" build-folder)))

(defn pom-data [opt]
  [[:url "https://github.com/chunsen-w/clojure-di"]
   [:licenses
    [:license
     [:name "Apache-2.0"]
     [:url "https://www.apache.org/licenses/LICENSE-2.0.txt"]]]
   [:scm
    [:url "https://github.com/chunsen-w/clojure-di"]
    [:connection "scm:git:https://github.com:chunsen-w/clojure-di.git"]
    [:developerConnection "scm:git:ssh:git@github.com:chunsen-w/clojure-di.git"]
    [:tag (str "v" (version opt))]]])

(defn jar [opt]
  (println opt)
  (clean nil)                                     ; clean leftovers 

  (b/copy-dir {:src-dirs   ["src" "resources"]    ; prepare jar content
               :target-dir jar-content})

  (b/compile-clj {:basis     basis               ; compile clojure code
                  :src-dirs  ["src"]
                  :class-dir jar-content
                  :filter-nses ['com.github.clojure.di]})

  (b/write-pom {:class-dir jar-content            ; create pom.xml
                :lib       lib-name
                :version   (or (:version opt) version)
                :basis     basis
                :src-dirs  ["src"]
                :pom-data  (pom-data opt)})

  (b/jar {:class-dir jar-content                  ; create jar
          :jar-file  (jar-file-name opt)})
  (println (format "Jar file created: \"%s\"" (jar-file-name opt))))

(defn publish-local [opt]
  (jar opt)
  (b/install {:class-dir jar-content            ; create pom.xml
              :lib       lib-name
              :jar-file  (jar-file-name opt)
              :basis     basis
              :version   version}))


(defn publish-clojar [opt]
  (jar opt)
  (dd/deploy {:installer :remote
              :artifact (jar-file-name opt)
              :pom-file (b/pom-path {:lib lib-name :class-dir jar-content})}))