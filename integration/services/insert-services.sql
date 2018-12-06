-- TEST:
\set test `cat ./test.cmdi`
INSERT INTO service
  ("name","recipe","semantics","kind")
SELECT
  'TEST', 'nl.knaw.meertens.deployment.lib.recipe.Test', :'test', 'service'
WHERE NOT EXISTS (
  SELECT "name" FROM service WHERE "name" = 'TEST'
);

-- VIEWER:
\set viewer `cat ./viewer.cmdi`
INSERT INTO service
  ("name", "recipe", "semantics", "kind")
SELECT
  'VIEWER', 'nl.knaw.meertens.deployment.lib.recipe.Text', :'viewer', 'viewer'
WHERE NOT EXISTS (
  SELECT "name" FROM service WHERE "name" = 'VIEWER'
);

