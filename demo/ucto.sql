\set ucto `cat /tmp/ucto.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('UCTO', 'nl.knaw.meertens.deployment.lib.Clam',  :'ucto', 'service');
\set frog `cat /tmp/frog.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('FROG', 'nl.knaw.meertens.deployment.lib.Clam',  :'frog', 'service');
\set viewer `cat /tmp/viewer.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('VIEWER', 'nl.knaw.meertens.deployment.lib.Text', :'viewer', 'viewer');
\set folia `cat /tmp/folia.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('FOLIA', 'nl.knaw.meertens.deployment.lib.Folia', :'folia', 'viewer');
\set foliaeditor `cat /tmp/foliaeditor.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('FOLIAEDITOR', 'nl.knaw.meertens.deployment.lib.FoliaEditor', :'foliaeditor', 'editor');
