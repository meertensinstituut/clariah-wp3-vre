\set tika `cat ./tika.cmdi`

INSERT INTO service
    ("name", "recipe", "semantics", "kind")
SELECT
       'TIKA', 'nl.knaw.meertens.deployment.lib.recipe.Tika', :'tika', 'service'
WHERE NOT EXISTS (
            SELECT "name" FROM service WHERE "name" = 'TIKA'
);
