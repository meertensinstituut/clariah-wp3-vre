\set demo `cat /tmp/demo.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('DEMO', 'nl.knaw.meertens.deployment.lib.Demo', :'demo', 'service');
