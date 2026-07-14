-- Spring Modulith's JDBC-backed event publication registry (the outbox
-- table backing ApplicationEventPublisher for every @ApplicationModuleListener
-- across all modules — see RULES.md Section 2.2). Schema copied verbatim
-- from spring-modulith-events-jdbc:2.0.7's bundled
-- org/springframework/modulith/events/jdbc/schemas/v2/schema-postgresql.sql,
-- per RULES.md Section 4.2's "ddl-auto: validate, Flyway owns every table"
-- discipline — this table is not exempt just because it belongs to a
-- framework rather than an application module.

CREATE TABLE IF NOT EXISTS event_publication
(
  id                     UUID NOT NULL,
  listener_id            TEXT NOT NULL,
  event_type             TEXT NOT NULL,
  serialized_event       TEXT NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash(serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);
