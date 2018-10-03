CREATE TABLE IF NOT EXISTS object (
    id bigserial PRIMARY KEY,
    type character varying(255) NOT NULL CHECK ( type IN ('object' ,'metadata' )),
    metadata_id bigint REFERENCES object (id),
    time_created timestamp with time zone,
    time_changed timestamp with time zone,
    filepath character varying(255) NOT NULL,
    filesize integer,
    fits xml,
    format character varying(255),
    mimetype character varying(255),
    user_id character varying(255) NOT NULL,
    deleted boolean NOT NULL
);

CREATE INDEX INDEX_OBJECT_ID ON object (id);
CREATE INDEX INDEX_OBJECT_USER_ID ON object (user_id);
CREATE INDEX INDEX_OBJECT_DELETED ON object (deleted);

CREATE OR REPLACE FUNCTION object_insert() RETURNS trigger AS $object_insert$
  BEGIN
    NEW.time_created = now();
    NEW.time_changed = null;
    IF (NEW.metadata_id IS NOT NULL) THEN
      PERFORM * FROM object WHERE id = NEW.metadata_id AND type = 'object';
      IF FOUND THEN
        RAISE EXCEPTION 'metadata_id should refer to metadata, not object';
      END IF;
    END IF;
    RETURN NEW;
  END;
$object_insert$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION object_update() RETURNS trigger AS $object_update$
  BEGIN
    NEW.time_changed = now();
    IF (NEW.metadata_id IS NOT NULL) THEN
      PERFORM * FROM object WHERE id = NEW.metadata_id AND type = 'object';
      IF FOUND THEN
        RAISE EXCEPTION 'metadata_id should refer to metadata, not object';
      END IF;
    END IF;
    IF (NEW.type = 'object') THEN
      PERFORM * FROM object WHERE metadata_id = NEW.id;
      IF FOUND THEN
        RAISE EXCEPTION 'metadata_id should refer to metadata, not object';
      END IF;
    END IF;
    RETURN NEW;
  END;
$object_update$ LANGUAGE plpgsql;

CREATE TRIGGER object_insert BEFORE INSERT ON object
FOR EACH ROW EXECUTE PROCEDURE object_insert();

CREATE TRIGGER object_update BEFORE UPDATE ON object
FOR EACH ROW EXECUTE PROCEDURE object_update();

CREATE VIEW user_file_count AS
  SELECT user_id, count(id) FROM object WHERE deleted=false GROUP BY user_id;