ALTER TABLE dictionary_unknownword
ADD COLUMN IF NOT EXISTS isIgnored tinyint(1) NOT NULL DEFAULT 0
AFTER isLocal;
