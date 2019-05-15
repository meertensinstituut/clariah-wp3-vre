\set frog `cat /tmp/frog.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('FROG', 'nl.knaw.meertens.deployment.lib.recipe.Clam',  :'frog', 'service');
\set folia `cat /tmp/folia.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('FOLIA', 'nl.knaw.meertens.deployment.lib.recipe.Folia', :'folia', 'viewer');
\set demo `cat /tmp/demo.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('DEMO', 'nl.knaw.meertens.deployment.lib.recipe.Demo', :'demo', 'service');
\set vupipeline `cat /tmp/vupipeline.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('VUPIPELINE', 'nl.knaw.meertens.deployment.lib.recipe.Clam', :'vupipeline', 'service');
