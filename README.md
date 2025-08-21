[![Clojars Project](https://img.shields.io/clojars/v/com.github.chunsen-w/clj-di.svg)](https://clojars.org/com.github.chunsen-w/clj-di)

## The basic idea
For a function, take it's arguments as dependencies, which will be injected automatically by the framework. And the return map as values it can provie to the injection context.

Eg:
```clojure
(defn a-fun [{:keys [db-port db-url]}]
  {:db-connection some-conn})
```
For this demo function, it means that give a `db-port` and a `db-url`, it will generate connection, and this connection can be injected by other functions with `db-connection`

## How to use
### Baic usage
```clojure
(require '[com.github.clojure.di.core :refer [defdi execute]])

(defdi http-server [{:keys [http-port route]
                     db-conn :db-connection}]
  ;start http server
  {:http-server xxx})

(defdi database [{:db/keys [url port user password]}]
  ; create db connection
  {:db-connection the-connection}）

;; other di
(defdi ...)

;;then run the di by
(execute [http-server database ...])
```
`defdi` has the same syntax as `defn`, and returns a normal function just as `defn`, except   
  1. It must have exactly one or zero argument
  2. It must return a map with keyword as keys

Then by run `(execute [http-server database ...])`,  it will calculate the dependency graph, and excute the di functions one by one with their dependency order

### Use `bootstrap`
For convenience, this library also provide a `bootstrap` function, to help you bootstrap you application 
```clojure
(require '[com.github.clojure.di.app :refer [bootstrap]])

(bootstrap "com.example")
```
Then, the libary will scan all the namespace in the classpath which has the prefix `com.example`, and collect all the di component defined by `defdi`, and excute them.   

*Thus, you don't need to care about where to import the depenedency, just let the library to handle it* 

For more detail usage, refer the [tests](https://github.com/wangchunsen/clojure-di/blob/main/test/com/github/clojure/di/core_test.clj)
