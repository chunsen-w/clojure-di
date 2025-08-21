(ns demo.demo1
  (:require [com.github.clojure.di.core :refer [defdi defcomp] :as di]
            [com.github.clojure.di.app :refer [bootstrap]]))

(defdi configs [_]
  #:conf{:db-url "demo-url"
         :db-user "demo-user"
         :db-pass "demo-pass"})

(defcomp db-conn [{:conf/keys [db-url db-user db-pass]}]
  ["I am the db conn" db-url db-user db-pass])

(defcomp http-server [{^:clj-di/list routes :http/route
                       db-conn ::db-conn}]
  (println "Routes are " routes)
  (assert (list? routes) "routes should be a list ")
  (println "db conn is " db-conn)
  ["I am the fake server"])

(defcomp void-init [_]
  (println "I am void init"))

(defdi route_1 [_]
  {:http/route [:get "/"]})

(defdi route_2 [_]
  {:http/route [:get "/api/2"]})


(defn -main [& args]
  (bootstrap ["demo.demo1"]))

(comment
  (-main)
  ;;
  )