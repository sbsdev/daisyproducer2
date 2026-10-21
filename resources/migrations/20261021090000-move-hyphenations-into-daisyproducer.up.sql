-- The hyphenations lived in the database of hyphen-keeper, the app that
-- maintained them before daisyproducer2 took that over, and were accessed
-- through a view. The view also made the test database share the
-- hyphenations with production. Move them into a table of their own.
-- hyphenation.words has a foreign key on the spelling to a table of its own,
-- which is not carried over, as the spelling is only ever 0 or 1.

CREATE TABLE hyphenation_words_new (
  word varchar(255) COLLATE utf8_bin NOT NULL,
  hyphenation varchar(255) COLLATE utf8_bin NOT NULL,
  spelling tinyint(4) NOT NULL,
  PRIMARY KEY (word, spelling),
  KEY hyphenation_words_spelling_index (spelling)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

--;;

INSERT INTO hyphenation_words_new (word, hyphenation, spelling)
SELECT word, hyphenation, spelling FROM hyphenation_words;

--;;

DROP VIEW hyphenation_words;

--;;

RENAME TABLE hyphenation_words_new TO hyphenation_words;
