(ns daisyproducer2.documents.schema-validation
  "Schema validation for XML"
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:import javax.xml.XMLConstants
           org.xml.sax.SAXException
           org.xml.sax.SAXParseException
           org.xml.sax.ErrorHandler
           org.w3c.dom.ls.LSResourceResolver
           org.w3c.dom.bootstrap.DOMImplementationRegistry
           org.w3c.dom.ls.DOMImplementationLS
           javax.xml.validation.SchemaFactory
           javax.xml.transform.stream.StreamSource))

(defn extract-error
  "Return a map with the error mesage and the line number given an `exception`"
  [^SAXParseException exception]
  {:error (.getMessage exception)
   :line (.getLineNumber exception)
   :column (.getColumnNumber exception)})

(defn stringify
  [{:keys [error line]}]
  (format "%s on line %d" error line))

(def resource-resolver
  "A resolver that helps finding schema files inside a Jar file. The
  `resolveResource` method of the `LSResourceresolver` interface
  accepts a number of params about a given resource and returns a
  `LSInput` object. This object will contain the proper `systemId` so
  that the resource is found inside the Jar file."
  (reify LSResourceResolver
    (resolveResource [this type namespaceURI publicId systemId baseURI]
      (let [;; the `systemId` contains the file name of the schema file we are
            ;; looking for. Given that we construct the path to the resource in
            ;; the Jar, create an io/resource and finally convert it to an URL.
            url (->>
                 systemId
                 (str "schema/")
                 io/resource
                 io/as-url)
            ;; `resolveResource` has to return an `LSInput` object that contains
            ;; enough information about the resource so that it can be found. So
            ;; we create an empty `LSInput` object and set the `systemId` to our
            ;; `url`. But to get an empty `LSInput` we'll first have to create a
            ;; `DOMImplementationRegistry`, then from that a `DOMImplementation`
            ;; which can be used to create a new `LSInput`.
            registry (DOMImplementationRegistry/newInstance)
            implementation (.getDOMImplementation registry "Core 3.0 XML 3.0 LS")
            input (doto (.createLSInput implementation)
                    ;; once we have the `LSInput` we can set the `systemId`
                    (.setSystemId (str url)))]
        input))))

(defn validation-errors
  "Return a list of validation errors when validating `file` against
  the given Relaxng `schema`. If the file is valid an empty list is
  returned. The errors are returned in the form a map contaings the
  keys :error, :line and :column"
  [file schema]
  ;; basically a minimal port of
  ;; http://stackoverflow.com/questions/15732/whats-the-best-way-to-validate-an-xml-file-against-an-xsd-file
  ;; also some inspiration came from
  ;; http://stackoverflow.com/questions/1541253/how-to-validate-an-xml-document-using-a-relax-ng-schema-and-jaxp
  (let [errors (atom [])
        error-handler (reify ErrorHandler
                        (fatalError [this exception]
                          (swap! errors conj (extract-error exception)))
                        (error [this exception]
                          (swap! errors conj (extract-error exception)))
                        (warning [this exception]))
        language XMLConstants/RELAXNG_NS_URI
        ;; We are using jing here because the plain validator (using
        ;; W3C_XML_SCHEMA_NS_URI) doesn't seem to work properly. It claims that
        ;; smil files are valid against the vubis bulk import schema.
        factory "com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory"
        schema-url (io/as-url (io/resource schema))
        validator (doto (.newValidator
                         (.newSchema
                          (doto (SchemaFactory/newInstance language factory nil)
                            ;; we have to sett a resource resolver on the schema
                            ;; factory so that it will find the (included)
                            ;; schema files inside the Jar file
                            (.setResourceResolver resource-resolver))
                          schema-url))
                    (.setErrorHandler error-handler))]
    (try
      (.validate validator (StreamSource. file))
      @errors
      (catch SAXException e
        (log/error (.getMessage e))
        @errors))))

(defn valid?
  "Check if a `file` is valid against given `schema`"
  [file schema]
  (empty? (validation-errors file schema)))
