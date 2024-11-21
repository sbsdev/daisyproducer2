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
                    :file "File"
                    :choose-file "Choose a file"
                    :choose-images "Choose images"
                    :no-file "No file selected"
                    :upload "Upload"
                    :image "Image"
                    :images "Images"
                    :version "Version"
                    :versions "Versions"
                    :new-version "Upload new version"
                    :new-image "Upload new images"
                    :upload-images "Upload Images"
                    :delete-all-images "Remove all images"
                    :products "Products"
                    ;;
                    :documents "Documents"
                    :confirm "Confirm"
                    :words "Words"
                    :book "Book"
                    ;;
                    :new "New"
                    :old "Old"
                    :open "In Production"
                    :closed "Finished"
                    :close "Close"
                    :reopen "Reopen"
                    :synchronize "Synchronize with archive"
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
                    :markup "Markup"
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
                    :large-print-library "Library"
                    :large-print-sale "Sale"
                    :large-print-configurable "Configurable"
                    :epub3 "EPUB3"
                    :online-player "Online Player"
                    :open-document "Open Document"
                    :html "HTML"
                    ;; Download Forms
                    :grade
                    {:g0 "Uncontracted"
                     :g1 "Grade 1"
                     :g2 "Grade 2"}
                    :accented-chars
                    {:basic "All Accents Reduced"
                     :swiss "Swiss Accents Detailed"}
                    :footnote-placement
                    {:standard "Standard"
                     :end-vol "At end of volume"
                     :level1 "At end of level1"
                     :level2 "At end of level2"
                     :level3 "At end of level3"
                     :level4 "At end of level4"}
                    :page-style
                    {:plain "Plain"
                     :with-page-nums "With Page Numbers"
                     :spacious "Spacious"
                     :scientific "Scientific"
                     }
                    :alignment
                    {:left "Left"
                     :justified "Justified"
                     }
                    :stock-size
                    {:a3paper "A3"
                     :a4paper "A4"
                     }
                    :line-spacing
                    {:singlespacing "Single spacing"
                     :onehalfspacing "One-and-a-half spacing"
                     :doublespacing "Double spacing"
                     }
                    :end-notes
                    {:none "Plain footnotes"
                     :document "Document Endnotes"
                     :chapter "Chapter Endnotes"
                     }
                    :image-visibility
                    {:show "Show Images"
                     :ignore "Hide Images"
                     }
                    :math
                    {:asciimath "AsciiMath"
                     :mathml "MathML"
                     :both "Both"
                     }
                    :image-inclusion
                    {:drop "None"
                     :link "Linked"
                     :embed "Embedded"
                     }
                    :forms
                    {
                     :cells-per-line "Cells per Line"
                     :lines-per-page "Lines per Page"
                     :contraction "Contraction"
                     :hyphenation "Hyphenation"
                     :toc-level "Depth of table of contents"
                     :footer-level "Footer up to level"
                     :include-macros "Include SBSForm macros"
                     :show-original-page-numbers "Show original page numbers"
                     :show-v-forms "Show V-Forms"
                     :downshift.ordinals "Downshift Ordinals"
                     :enable-capitalization "Enable Capitalization"
                     :accented-chars "Accented Characters"
                     :footnote-placement "Placement of Footnotes"
                     :font-size "Font Size"
                     :font "Font"
                     :page-style "Page Style"
                     :alignment "Alignment"
                     :stock-size "Stock Size"
                     :line-spacing "Line Spacing"
                     :replace-em-with-quote "Replace italics with quote"
                     :end-notes "End Notes"
                     :image-visibility "Image Visibility"
                     :math "Math"
                     :phonetics "Phonetics"
                     :image-inclusion "Images"
                     :line-numbers "Line numbers"
                     :page-numbers "Page numbers"
                     :floating-page-numbers "Floating page numbers"
                     :answer-markup "Answer markup"
                     }
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
                    :file "Datei"
                    :choose-file "Datei wählen"
                    :choose-images "Bilder wählen"
                    :no-file "Keine Datei ausgewählt"
                    :upload "Hochladen"
                    :image "Bild"
                    :images "Bilder"
                    :version "Version"
                    :versions "Versionen"
                    :new-version "Neue Version hochladen"
                    :new-image "Neue Bilder hochladen"
                    :upload-images "Bilder hochladen"
                    :delete-all-images "Alle Bilder löschen"
                    :products "Produkte"
                    ;;
                    :documents "Dokumente"
                    :confirm "Bestätigen"
                    :words "Globale Wörter"
                    :book "Buch"
                    ;;
                    :new "Neu"
                    :open "In Produktion"
                    :closed "Fertig"
                    :close "Abschliessen"
                    :reopen "Wiedereröffnen"
                    :synchronize "Letzte Version vom Archiv holen"
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
                    :markup "Markup"
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
                    :large-print-library "Ausleih"
                    :large-print-sale "Verkauf"
                    :large-print-configurable "Konfigurierbar"
                    :epub3 "EPUB3"
                    :online-player "Online Player"
                    :open-document "Open Document"
                    :html "HTML"
                    ;; Download Forms
                    :grade
                    {:g0 "Basisschrift"
                     :g1 "Vollschrift"
                     :g2 "Kurzschrift"}
                    :accented-chars
                    {:basic "Alle nur mit Grundbuchstaben"
                     :swiss "Schweizer Akzente ausführlich"}
                    :footnote-placement
                    {:standard "Standard"
                     :end-vol "Am Bandende"
                     :level1 "Am Ende von level1"
                     :level2 "Am Ende von level2"
                     :level3 "Am Ende von level3"
                     :level4 "Am Ende von level4"}
                    :page-style
                    {:plain "Einfach"
                     :with-page-nums "Mit Original-Seitenzahlen"
                     :spacious "Level2 in bodymatter immer auf neuer Seite"
                     :scientific "Wissenschaftlich"
                     }
                    :alignment
                    {:left "Linksbündig"
                     :justified "Blocksatz"
                     }
                    :stock-size
                    {:a3paper "A3"
                     :a4paper "A4"
                     }
                    :line-spacing
                    {:singlespacing "Single spacing"
                     :onehalfspacing "One-and-a-half spacing"
                     :doublespacing "Double spacing"
                     }
                    :end-notes
                    {:none "Normale Fussnoten"
                     :document "Endnotes am Ende des Dokuments"
                     :chapter "Endnotes am Ende des Kapitels"
                     }
                    :image-visibility
                    {:show "Show Images"
                     :ignore "Hide Images"
                     }
                    :math
                    {:asciimath "AsciiMath"
                     :mathml "MathML"
                     :both "Beide"
                     }
                    :image-inclusion
                    {:drop "Ohne"
                     :link "Linked"
                     :embed "Embedded"
                     }
                    :forms
                    {:cells-per-line "Zellen pro Zeile"
                     :lines-per-page "Zeilen pro Seite"
                     :contraction "Kürzungsgrad"
                     :hyphenation "Silbentrennung"
                     :toc-level "Angezeigte Ebenen im Inhaltsverzeichnis"
                     :footer-level "Angezeigte Ebenen im Laufindex"
                     :include-macros "SBSForm-Makros mitliefern"
                     :show-original-page-numbers "Schwarzschrift-Seitenzahlen anzeigen"
                     :show-v-forms "Höflichkeitsformen anzeigen"
                     :downshift-ordinals "Ordnungszahlen herabsetzen"
                     :enable-capitalization "Grossschreibung einschalten"
                     :accented-chars "Akzentbuchstaben"
                     :footnote-placement "Anordnung der Fussnoten"
                     :font-size "Fontgrösse"
                     :font "Font"
                     :page-style "Seitenstil"
                     :alignment "Alignment"
                     :stock-size "Papiergrösse"
                     :line-spacing "Zeilenabstabstand"
                     :replace-em-with-quote "Kursiv mit Apostrophe ersetzen"
                     :end-notes "End Notes"
                     :image-visibility "Bilder"
                     :math "Math"
                     :phonetics "Phonetics"
                     :image-inclusion "Images"
                     :line-numbers "Line numbers"
                     :page-numbers "Page numbers"
                     :floating-page-numbers "Floating page numbers"
                     :answer-markup "Antwort Markup"}
                    }
                   })

(def tr (partial tempura/tr {:dict translations :default-locale :en} [:de]))
