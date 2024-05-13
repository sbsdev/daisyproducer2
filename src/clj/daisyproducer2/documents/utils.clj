(ns daisyproducer2.documents.utils
  (:require [babashka.fs :as fs]))

(defmacro with-tempfile
  [[tempfile tempfile-opts] & body]
  `(let [~tempfile (fs/create-temp-file ~tempfile-opts)]
     (try
       ~@body
       (finally
         (fs/delete ~tempfile)))))

