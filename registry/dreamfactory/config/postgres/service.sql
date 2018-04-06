CREATE TABLE IF NOT EXISTS service (
    id bigserial PRIMARY KEY,
    name character varying(255) NOT NULL,
    recipe character varying(255) NOT NULL,
    semantics xml,
    tech xml,
    time_created timestamp with time zone,
    time_changed timestamp with time zone
);

CREATE OR REPLACE FUNCTION service_insert() RETURNS trigger AS $service_insert$
  BEGIN
    NEW.time_created = now();
    NEW.time_changed = null;
    RETURN NEW;
  END;
$service_insert$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION service_update() RETURNS trigger AS $service_update$
  BEGIN
    NEW.time_changed = now();
    RETURN NEW;
  END;
$service_update$ LANGUAGE plpgsql;

CREATE TRIGGER service_insert BEFORE INSERT ON service
FOR EACH ROW EXECUTE PROCEDURE service_insert();

CREATE TRIGGER service_update BEFORE UPDATE ON service
FOR EACH ROW EXECUTE PROCEDURE service_update();