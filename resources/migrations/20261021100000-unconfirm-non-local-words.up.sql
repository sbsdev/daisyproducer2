-- Local words that were confirmed and then saved as not local kept their
-- confirmation (#154). They were neither in the global words nor up for
-- confirmation. Unconfirm them, so that they can be confirmed again.

UPDATE dictionary_localword
SET isConfirmed = FALSE
WHERE isLocal = FALSE AND isConfirmed = TRUE;
