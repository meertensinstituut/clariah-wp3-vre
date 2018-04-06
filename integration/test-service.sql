INSERT INTO service
  ("name","recipe","semantics")
SELECT
  'TEST', 'nl.knaw.meertens.deployment.lib.Test',  ''
WHERE NOT EXISTS (
  SELECT "name" FROM service WHERE "name" = 'TEST'
);
