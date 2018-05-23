\set ucto `cat /tmp/ucto.xml`
INSERT INTO service("name","recipe","semantics") VALUES ('UCTO', 'nl.knaw.meertens.deployment.lib.Clam',  :'ucto');
\set frog `cat /tmp/frog.xml`
INSERT INTO service("name","recipe","semantics") VALUES ('FROG', 'nl.knaw.meertens.deployment.lib.Clam',  :'frog');