-- change all (non-Swiss) accents in the braille columns to plain characters preceded by dot 6
UPDATE dictionary_globalword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)á|ã|å', '"A'), contracted = REGEXP_REPLACE(contracted, '(?i)á|ã|å', '"A') WHERE untranslated LIKE "%á%" OR untranslated LIKE "%ã%" OR untranslated LIKE "%å%";

--;;

UPDATE dictionary_globalword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)æ', 'AE'), contracted = REGEXP_REPLACE(contracted, '(?i)æ', 'AE') WHERE untranslated LIKE "%æ%";

--;;

UPDATE dictionary_globalword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)í', '"I'), contracted = REGEXP_REPLACE(contracted, '(?i)í', '"I') WHERE untranslated LIKE "%í%";

--;;

UPDATE dictionary_globalword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)ð', '"D'), contracted = REGEXP_REPLACE(contracted, '(?i)ð', '"D')  WHERE untranslated LIKE "%ð%";

--;;

UPDATE dictionary_globalword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)ñ', '"N'), contracted = REGEXP_REPLACE(contracted, '(?i)ñ', '"N') WHERE untranslated LIKE "%ñ%";

--;;

UPDATE dictionary_globalword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)ó|õ|ø', '"O'), contracted = REGEXP_REPLACE(contracted, '(?i)ó|õ|ø', '"O') WHERE untranslated LIKE "%ó%" OR untranslated LIKE "%õ%" OR untranslated LIKE "%ø%";

--;;

UPDATE dictionary_globalword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)ú', '"U'), contracted = REGEXP_REPLACE(contracted, '(?i)ú', '"U') WHERE untranslated LIKE "%ú%";

--;;

UPDATE dictionary_globalword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)ý|ÿ', '"Y'), contracted = REGEXP_REPLACE(contracted, '(?i)ý|ÿ', '"Y') WHERE untranslated LIKE "%ý%" OR untranslated LIKE "%ÿ%";

--;;

UPDATE dictionary_globalword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)þ', '"T'), contracted = REGEXP_REPLACE(contracted, '(?i)þ', '"T') WHERE untranslated LIKE "%þ%";

--;;

UPDATE dictionary_localword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)á|ã|å', '"A'), contracted = REGEXP_REPLACE(contracted, '(?i)á|ã|å', '"A') WHERE untranslated LIKE "%á%" OR untranslated LIKE "%ã%" OR untranslated LIKE "%å%";

--;;

UPDATE dictionary_localword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)æ', 'AE'), contracted = REGEXP_REPLACE(contracted, '(?i)æ', 'AE') WHERE untranslated LIKE "%æ%";

--;;

UPDATE dictionary_localword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)í', '"I'), contracted = REGEXP_REPLACE(contracted, '(?i)í', '"I') WHERE untranslated LIKE "%í%";

--;;

UPDATE dictionary_localword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)ð', '"D'), contracted = REGEXP_REPLACE(contracted, '(?i)ð', '"D')  WHERE untranslated LIKE "%ð%";

--;;

UPDATE dictionary_localword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)ñ', '"N'), contracted = REGEXP_REPLACE(contracted, '(?i)ñ', '"N') WHERE untranslated LIKE "%ñ%";

--;;

UPDATE dictionary_localword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)ó|õ|ø', '"O'), contracted = REGEXP_REPLACE(contracted, '(?i)ó|õ|ø', '"O') WHERE untranslated LIKE "%ó%" OR untranslated LIKE "%õ%" OR untranslated LIKE "%ø%";

--;;

UPDATE dictionary_localword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)ú', '"U'), contracted = REGEXP_REPLACE(contracted, '(?i)ú', '"U') WHERE untranslated LIKE "%ú%";

--;;

UPDATE dictionary_localword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)ý|ÿ', '"Y'), contracted = REGEXP_REPLACE(contracted, '(?i)ý|ÿ', '"Y') WHERE untranslated LIKE "%ý%" OR untranslated LIKE "%ÿ%";

--;;

UPDATE dictionary_localword SET uncontracted = REGEXP_REPLACE(uncontracted, '(?i)þ', '"T'), contracted = REGEXP_REPLACE(contracted, '(?i)þ', '"T') WHERE untranslated LIKE "%þ%";
