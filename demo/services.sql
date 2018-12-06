\set ucto `cat /tmp/ucto.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('UCTO', 'nl.knaw.meertens.deployment.lib.recipe.Clam',  :'ucto', 'service');
\set frog `cat /tmp/frog.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('FROG', 'nl.knaw.meertens.deployment.lib.recipe.Clam',  :'frog', 'service');
\set viewer `cat /tmp/viewer.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('VIEWER', 'nl.knaw.meertens.deployment.lib.recipe.Text', :'viewer', 'viewer');
\set folia `cat /tmp/folia.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('FOLIA', 'nl.knaw.meertens.deployment.lib.recipe.Folia', :'folia', 'viewer');
\set foliaeditor `cat /tmp/foliaeditor.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('FOLIAEDITOR', 'nl.knaw.meertens.deployment.lib.recipe.FoliaEditor', :'foliaeditor', 'editor');
\set demo `cat /tmp/demo.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('DEMO', 'nl.knaw.meertens.deployment.lib.recipe.Demo', :'demo', 'service');
