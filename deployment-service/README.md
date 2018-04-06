Deployment service
===

Deployment
---

- Add saxon utils to local repo:
```
cd ./saxon-utils
mvn install:install-file \
  -Dfile=SaxonUtils-1.0-SNAPSHOT.jar \
  -DgroupId=nl.knaw.meertens.deployment \
  -DartifactId=deployment-service \
  -Dversion=1.0-SNAPSHOT \
  -Dpackaging=jar \
  -DgeneratePom=true
  
```
- To build war-file, run `mvn clean install`
- Run `cd ../ ; ./start-vre.sh`

Status codes
---
```
PUT /exec/<service>/<workdir>   200    created
PUT /exec/<service>/<workdir>   403    already running
GET /exec/task/<workdir>        202    running
GET /exec/task/<workdir>        200    finished
GET /exec/task/<workdir>        404    service not found
```