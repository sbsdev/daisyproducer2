-- Go back to the view on the database of hyphen-keeper. The table is kept
-- under another name, as it holds the changes made since the migration,
-- which are not in hyphen-keeper's database. Creating the view needs read
-- access to that database.

RENAME TABLE hyphenation_words TO hyphenation_words_moved;

--;;

CREATE VIEW hyphenation_words AS
SELECT word, hyphenation, spelling FROM hyphenation.words;
