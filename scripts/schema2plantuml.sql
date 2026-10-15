-- Generate a PlantUML entity relationship diagram of the current database
--
--   mysql -N -r -h db.sbszh.ch -P 3309 -u <user> -p <db> < scripts/schema2plantuml.sql > doc/db_schema.puml

SET SESSION group_concat_max_len = 1000000;

SELECT '@startuml\n\n'' avoid problems with angled crows feet\nskinparam linetype ortho\nhide circle\n';

-- one entity per table, primary key first, foreign keys marked
SELECT CONCAT('entity "', c.table_name, '" {\n',
              CONCAT_WS('\n  --\n',
                        GROUP_CONCAT(IF(c.column_key = 'PRI',
                                        CONCAT('  *', c.column_name, ' : ', c.column_type,
                                               IF(k.referenced_table_name IS NOT NULL, ' <<FK>>', '')),
                                        NULL)
                                     ORDER BY c.ordinal_position SEPARATOR '\n'),
                        GROUP_CONCAT(IF(c.column_key = 'PRI',
                                        NULL,
                                        CONCAT('  ', c.column_name, ' : ', c.column_type,
                                               IF(k.referenced_table_name IS NOT NULL, ' <<FK>>', '')))
                                     ORDER BY c.ordinal_position SEPARATOR '\n')),
              '\n}\n')
FROM information_schema.columns c
LEFT JOIN information_schema.key_column_usage k
  ON k.table_schema = c.table_schema AND k.table_name = c.table_name
 AND k.column_name = c.column_name AND k.referenced_table_name IS NOT NULL
WHERE c.table_schema = DATABASE() AND c.table_name <> 'schema_migrations'
GROUP BY c.table_name
ORDER BY c.table_name;

-- one relation per foreign key
SELECT CONCAT(referenced_table_name, ' ||--o{ ', table_name)
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE() AND referenced_table_name IS NOT NULL
ORDER BY referenced_table_name, table_name;

SELECT '\n@enduml';
