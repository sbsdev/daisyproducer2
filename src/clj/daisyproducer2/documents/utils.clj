(ns daisyproducer2.documents.utils
  (:require [babashka.fs :as fs]))

(defmacro with-tempfile
  [[tempfile opts] & body]
  `(let [~tempfile (fs/create-temp-file ~opts)]
     (try
       ~@body
       (finally
         (fs/delete ~tempfile)))))

(defmacro with-tempdir
  [[tempdir opts] & body]
  `(let [~tempdir (fs/create-temp-dir ~opts)]
     (try
       ~@body
       (finally
         (fs/delete-tree ~tempdir)))))
