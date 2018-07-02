\set uitestservice `cat /tmp/ui-test-service.xml`
INSERT INTO service("name","recipe","semantics") VALUES ('UI-TEST', 'nl.knaw.meertens.deployment.lib.Test',  :'uitestservice');
