(ns daisyproducer2.uuid
  (:import com.github.f4b6a3.uuid.alt.GUID))

(defn uuid
  "Generate a Unix Epoch time-based UUID (UUID version 7) according to [RFC 9562](https://www.rfc-editor.org/rfc/rfc9562)"
  []
  (str (GUID/v7)))
