(ns daisyproducer2.i18n
  (:require [taoensso.tempura :as tempura]))

(def translations {:en
                   {:missing "missing translation"
                    :approve "Approve"
                    :approve-all "Approve all"
                    :save "Save"
                    :delete "Delete"
                    :cancel "Cancel"
                    :edit "Edit"
                    :insert "Insert"
                    :ignore "Ignore"
                    :download "Download"
                    :input-not-valid "Input not valid"
                    :unknown "Unknown"
                    :untranslated "Untranslated"
                    :uncontracted "Uncontracted"
                    :contracted "Contracted"
                    :hyphenated "Hyphenated"
                    :hyphenated-with-spelling "Hyphenated (%1)"
                    :type "Type"
                    :homograph-disambiguation "Homograph disambiguation"
                    :local "Local"
                    :action "action"
                    :search "Search"
                    :both-grades "Both"
                    :loading "Loading..."
                    :login "Log in"
                    :logout "Log out"
                    :username "Username"
                    :password "Password"
                    :comment "Comment"
                    :choose-dtbook "Choose a DTBook file"
                    :choose-images "Choose images"
                    :no-file "No file selected"
                    :upload "Upload"
                    :image "Image"
                    :images "Images"
                    :version "Version"
                    :versions "Versions"
                    ;;
                    :documents "Documents"
                    :confirm "Confirm"
                    :words "Words"
                    :book "Book"
                    ;;
                    :new "New"
                    :old "Old"
                    :in-production "In Production"
                    :finished "Finished"
                    ;;
                    :title "Title"
                    :author "Author"
                    :source-publisher "Source Publisher"
                    :state "State"
                    :language "language"
                    :spelling "Spelling"
                    :date "Date"
                    :modified-at "Last modified at"
                    :created-at "Created at"
                    ;;
                    :transitions-state "Transition state to '%1'"
                    ;;
                    :details "Details"
                    :unknown-words "Unknown Words"
                    :local-words "Local Words"
                    :preview "Preview"
                    :format "Format"
                    ;;
                    :old-spelling "Old spelling"
                    :new-spelling "New spelling"
                    :unknown-spelling "Unknown spelling"
                    ;;
                    :type-none "None"
                    :type-name "Name"
                    :type-name-hoffmann "Name (Type Hoffmann)"
                    :type-place "Place"
                    :type-place-langenthal "Place (Type Langenthal)"
                    :type-homograph "Homograph"
                    ;;
                    :something-bad-happened "Something very bad has happened!"
                    :something-bad-happened-message "We've dispatched a team of highly trained gnomes to take care of the problem."
                    :invalid-anti-forgery-token "Invalid anti-forgery token"
                    :not-authenticated "Access to %1 only for logged-in users"
                    :not-authorized "Access to %1 is not authorized"
                    ;;
                    :previous "Previous"
                    :next "Next"
                    :word "Word"
                    :hyphenation "Hypenation"
                    :hyphenations "Hypenations"
                    :suggested-hyphenation "Suggested Hyphenation"
                    :corrected-hyphenation "Corrected Hyphenation"
                    :lookup-hyphenation "Lookup"
                    :already-defined-hyphenation "Word has already been defined. Use Edit to change it"
                    :same-as-suggested-hyphenation "The hyphenation is the same as the suggestion"
                    ;;
                    :braille "Braille"
                    :large-print "Large Print"
                    :epub3 "EPUB3"
                    :open-in-online-player "Open in Online Player"
                    }
                   :de
                   {:missing "Fehlende Übersetzung"
                    :approve "Bestätigen"
                    :approve-all "Alle Bestätigen"
                    :save "Speichern"
                    :delete "Löschen"
                    :cancel "Abbrechen"
                    :edit "Editieren"
                    :insert "Einfügen"
                    :ignore "Ignorieren"
                    :download "Herunterladen"
                    :input-not-valid "Eingabe ungültig"
                    :unknown "Unbekannt"
                    :untranslated "Schwarzschrift"
                    :uncontracted "Vollschrift"
                    :contracted "Kurzschrift"
                    :hyphenated "Trennung"
                    :hyphenated-with-spelling "Trennung (%1)"
                    :type "Markup"
                    :homograph-disambiguation "Homograph Präzisierung"
                    :local "Lokal"
                    :action "Aktion"
                    :search "Suche"
                    :both-grades "Beide"
                    :loading "Laden..."
                    :login "Anmelden"
                    :logout "Abmelden"
                    :username "Benutzername"
                    :password "Passwort"
                    :comment "Kommentar"
                    :choose-dtbook "DTBook Datei wählen"
                    :choose-images "Bilder wählen"
                    :no-file "Keine Datei ausgewählt"
                    :upload "Hochladen"
                    :image "Bild"
                    :images "Bilder"
                    :version "Version"
                    :versions "Versionen"
                    ;;
                    :documents "Dokumente"
                    :confirm "Bestätigen"
                    :words "Globale Wörter"
                    :book "Buch"
                    ;;
                    :new "Neu"
                    :in-production "In Produktion"
                    :finished "Fertig"
                    ;;
                    :title "Titel"
                    :author "Autor"
                    :source-publisher "Verlag"
                    :state "Status"
                    :language "Sprache"
                    :spelling "Rechtschreibung"
                    :date "Datum"
                    :modified-at "Letzte Änderung"
                    :created-at "Erstellt am"
                    ;;
                    :transitions-state "Status auf '%1' setzen"
                    ;;
                    :details "Details"
                    :unknown-words "Unbekannte Wörter"
                    :local-words "Lokale Wörter"
                    :preview "Vorschau"
                    :format "Format"
                    ;;
                    :old-spelling "Alte Rechtschreibung"
                    :new-spelling "Neue Rechtschreibung"
                    :unknown-spelling "Rechtschreibung unbekannt"
                    ;;
                    :type-none ""
                    :type-name "Name"
                    :type-name-hoffmann "Name (Type Hoffmann)"
                    :type-place "Place"
                    :type-place-langenthal "Place (Type Langenthal)"
                    :type-homograph "Homograph"
                    ;;
                    :something-bad-happened "Ein Problem ist aufgetreten!"
                    :something-bad-happened-message "Die IT ist informiert und arbeitet an der Behebung."
                    :invalid-anti-forgery-token "Invalid anti-forgery token"
                    :not-authenticated "Zugriff auf %1 nur für angemeldete Benutzer"
                    :not-authorized "Zugriff auf %1 nicht gestattet"
                    ;;
                    :previous "Vorherige"
                    :next "Nächste"
                    :word "Wort"
                    :hyphenation "Trennung"
                    :hyphenations "Trennungen"
                    :suggested-hyphenation "Vorgeschlagene Trennung"
                    :corrected-hyphenation "Korrigierte Trennung"
                    :lookup-hyphenation "Nachschlagen"
                    :already-defined-hyphenation "Die Trennung ist schon definiert. Bitte benutzen Sie 'Editieren' um sie zu ändern"
                    :same-as-suggested-hyphenation "Die Trennung ist gleich wie der Trenn-Vorschlag"
                    ;;
                    :braille "Braille"
                    :large-print "Grossdruck"
                    :epub3 "EPUB3"
                    :open-in-online-player "Im Online Player öffnen"
                    }
                   })

(def tr (partial tempura/tr {:dict translations :default-locale :en} [:de]))
