\set recipe `cat ./recipe.cmdi`

INSERT INTO service
    ("name", "recipe", "semantics", "kind")
SELECT
       'TEXTSTATS', 'nl.knaw.meertens.deployment.lib.recipe.Textstats', :'recipe', 'service'
WHERE NOT EXISTS (
            SELECT "name" FROM service WHERE "name" = 'TEXTSTATS'
);
