\set uitestservice `cat /tmp/ui-test-service.xml`
INSERT INTO service("name","recipe","semantics","kind") VALUES ('UI-TEST', 'nl.knaw.meertens.deployment.lib.Test',  :'uitestservice', 'service');
