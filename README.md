[![Clojars Project](https://img.shields.io/clojars/v/com.github.chunsen-w/clj-di.svg)](https://clojars.org/com.github.chunsen-w/clj-di)

## The basic idea
```clojure
(defn a-fun [{:keys [db-port db-url]}]
  {:db-connection some-conn})
```
For this function, it is known that given `db-port` and `db-url` it will given back a `db-connection`.  We can use clojure macro to parse these information

## How to use
### Baic usage
```clojure
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
  1. It must have exactly one argument
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
