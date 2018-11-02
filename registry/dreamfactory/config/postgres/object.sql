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

-- tag:
CREATE TABLE IF NOT EXISTS tag (
  id bigserial PRIMARY KEY,
  name character varying(255),
  type character varying(255),
  owner character varying(255),
  unique (name, type, owner)
);

-- object tag link:
CREATE TABLE IF NOT EXISTS object_tag (
  id bigserial PRIMARY KEY,
  tag BIGINT REFERENCES tag (id),
  object BIGINT REFERENCES object (id),
  created timestamp with time zone,
  unique (tag, object)
);

CREATE INDEX INDEX_TAG_OBJECT_OBJECT
  ON object_tag (object);
CREATE INDEX INDEX_TAG_OBJECT_TAG
  ON object_tag (tag);

-- when linking tag to object, check owners match:
CREATE OR REPLACE FUNCTION insert_object_tag(_tag BIGINT, _object BIGINT, _owner TEXT, OUT id BIGINT) AS
$BODY$
BEGIN
  IF NOT EXISTS(SELECT * FROM tag WHERE tag.id = _tag AND tag.owner = _owner) THEN
    RAISE EXCEPTION 'tag [%] is not owned by [%]', _tag, _owner;
  END IF;

  INSERT INTO object_tag(tag, object, created)
  VALUES(_tag, _object, current_timestamp)
  RETURNING object_tag.id INTO id;
END;
$BODY$
LANGUAGE plpgsql;

-- view object with full tags:
CREATE VIEW object_full_tag AS
  SELECT
    object_tag.object,
    object_tag.tag,
    object_tag.created,
    tag.name,
    tag.type,
    tag.owner
  FROM object_tag
    LEFT JOIN tag ON tag.id = object_tag.tag;

