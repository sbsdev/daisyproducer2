(ns daisyproducer2.i18n
  (:require [taoensso.tempura :as tempura]))

(def translations {:en
                   {:missing "missing translation"
                    :approve "Approve"
                    :approve-all "Approve all"
                    :save "Save"
                    :delete "Delete"
                    :ignore "Ignore"
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
                    ;;
                    :documents "Documents"
                    :confirm "Confirm"
                    :words "Words"
                    :book "Book"
                    ;;
                    :new "New"
                    :in-production "In Production"
                    :finished "Finished"
                    ;;
                    :title "Title"
                    :author "Author"
                    :source-publisher "Source Publisher"
                    :state "State"
                    ;;
                    :details "Details"
                    :unknown-words "Unknown Words"
                    :local-words "Local Words"
                    ;;
                    :spelling
                    {:title "Spelling"
                     :old "Old spelling"
                     :new "New spelling"
                     :unknown "Unknown spelling"
                     :old-brief "Old"
                     :new-brief "New"
                     :unknown-brief "Unknown"
                     }
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
                    }
                   :de
                   {:missing "Fehlende Übersetzung"
                    :approve "Bestätigen"
                    :approve-all "Alle Bestätigen"
                    :save "Speichern"
                    :delete "Löschen"
                    :ignore "Ignorieren"
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
                    ;;
                    :details "Details"
                    :unknown-words "Unbekannte Wörter"
                    :local-words "Lokale Wörter"
                    ;;
                    :spelling
                    {:title "Rechtschreibung"
                     :old "Alte Rechtschreibung"
                     :new "Neue Rechtschreibung"
                     :unknown "Rechtschreibung unbekannt"
                     :old-brief "Alt"
                     :new-brief "Neu"
                     :unknown-brief "Unbekannt"
                     }
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
                    }})

(def tr (partial tempura/tr {:dict translations :default-locale :en} [:de]))
