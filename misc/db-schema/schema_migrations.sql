CREATE TABLE schema_migrations (
  id bigint(20) NOT NULL,
  applied timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  description varchar(1024) DEFAULT NULL,
  UNIQUE KEY id (id)
);
