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
  IF NOT EXISTS(SELECT * FROM tag WHERE tag.id = _tag AND tag.owner IN (_owner, 'system')) THEN
    RAISE EXCEPTION 'tag [%] is not owned by system or [%]', _tag, _owner;
  END IF;

  INSERT INTO object_tag(tag, object, created)
  VALUES(_tag, _object, current_timestamp)
  RETURNING object_tag.id INTO id;
END;
$BODY$
LANGUAGE plpgsql;

-- update object tag; tag id found by:
--  -object id,
--  -old tag type,
--  -and old tag owner
CREATE OR REPLACE FUNCTION update_object_tag(
  _object BIGINT, _new_tag BIGINT, _type TEXT, _owner TEXT, OUT id BIGINT
) AS $BODY$
DECLARE
  tag_count     INT;
  object_tag_id BIGINT;
BEGIN
  SELECT INTO tag_count COUNT(*)
  FROM object_tag
         LEFT JOIN tag ON object_tag.tag = tag.id
  WHERE object_tag.object = _object
    AND tag.type = _type
    AND tag.owner = _owner;

  IF tag_count < 1
  THEN
    RAISE EXCEPTION 'Could not update object tag: no tag found for object %, tag type % and owner %', _object, _type, _owner;
  END IF;
  IF tag_count > 1
  THEN
    RAISE EXCEPTION 'Could not update object tag: multiple tags found for object %, tag type % and owner %', _object, _type, _owner;
  END IF;

  SELECT INTO object_tag_id object_tag.id
  FROM object_tag
         LEFT JOIN tag ON object_tag.tag = tag.id
  WHERE object_tag.object = _object
    AND tag.type = _type;

  UPDATE object_tag SET tag = _new_tag WHERE object_tag.id = object_tag_id;

  id := object_tag_id;
END;
$BODY$
LANGUAGE plpgsql;

-- view object with full tags:
CREATE OR REPLACE VIEW object_full_tag AS
  SELECT
    object_tag.object,
    object_tag.tag,
    object_tag.created,
    tag.name,
    tag.type,
    tag.owner
  FROM object_tag
    LEFT JOIN tag ON tag.id = object_tag.tag;

-- object and its semantic types:
-- tag:
CREATE TABLE IF NOT EXISTS object_semantic_type (
  id bigserial PRIMARY KEY,
  object_id bigserial,
  semantic_type character varying(255),
  unique (object_id, semantic_type)
);
CREATE INDEX IF NOT EXISTS INDEX_OBJECT_SEMANTIC_TYPE_OBJECT_ID ON object_semantic_type (object_id);
CREATE INDEX IF NOT EXISTS INDEX_OBJECT_SEMANTIC_TYPE_SEMANTIC_TYPE ON object_semantic_type (semantic_type);
