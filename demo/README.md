# CLARIAH - VRE - demo

0. [X] Start [LaMachine](https://proycon.github.io/LaMachine/)
   - This automatically downloads and starts a LaMachine VM
```
vagrant init proycon/lamachine
vagrant up
```

0. [X] Make sure the VRE doesn't have clashing ports
  - change the port mapping in `recognizer/docker-compose.yml` into
```yml
expose:
  - 80
```
0. [X] Start the VRE
```sh
./start-vre.sh
```
0. [X] Populate the Service Registry
```sh
docker cp ucto.cmdi vre_postgres_1:/tmp/ucto.xml
docker cp ucto.sql vre_postgres_1:/tmp/ucto.sql
docker exec -it vre_postgres_1 psql -U services services -f /tmp/ucto.sql
```
Or
```sh
./VRE-demo-init.sh
```

1. [X] Upload a sample text file
  - create a fresh one with a news item from nos.nl
```sh
vi nos.txt
curl -v 'http://localhost:8082/remote.php/webdav/nos.txt' \
     -X PUT \
     -H 'Content-Type: text/plain; charset=UTF-8' \
     -u admin:admin \
     -d '@nos.txt'
```
2. [X] Show the file in OwnCloud
  - goto http://localhost:8082/
  - login `admin:admin`
3. [X] Show the message in Kafka
  - goto http://localhost:9000/#/observe
4. [X] Show the entry in the Object Registry
  - goto http://localhost:8089
  - login `noreply@dreamfactory.dev:password`
  - goto `Data > Objects VRE > Object`
  - click `Set Service`
  - enable all fields in the `Fields` tab
  - remember the object id (OID)

Or for the upload steps above:
```sh
vi nos.txt
./VRE-demo-upload.sh
```

10. [X] Show the entries in the Service Registry
  - goto http://localhost:8089
  - (login `noreply@dreamfactory.dev:password`)
  - goto `Data > Services VRE > Service`
  - click `Set Service`
  - enable all fields in the `Fields` tab
11. [X] Trigger `ucto` via the Switchboard
```sh
curl -X POST http://localhost:9010/switchboard/rest/exec/UCTO -H "Content-Type: application/json" -d '{ "params": [ { "name": "untokinput", "type": "file", "value": OID, "params": [ { "language": "nld", "author": "F. Emmer" } ] } ] }'
```
  - remember the work directory (WD)
12. [X] Poll if it's finished
```sh
curl http://localhost:9010/switchboard/rest/exec/task/WD
```

Or for the execution steps above
```sh
./VRE-demo-exec.sh OID
```

13. [X] Show the project in LaMachine
  - http://127.0.0.1:8080/ucto/
14. [X] Show the output in OwnCloud
  - goto http://localhost:8082/
  - (login `admin:admin`)

__NOTE__: the demo currently (april 2018) breaks here due to a failing trigger!

20. [ ] Show the entries in the Solr
  - http://localhost:8087/solr/#/vrecore/query
    - set sort to `created desc`
  - http://localhost:8087/solr/vrecore/select?q=mtas_text:SEARCHTERM
21. [ ] Run a CQL query
  - `http://localhost:8087/solr/vrecore/select?q=mtas_text:de`
  - `http://localhost:8087/solr/vrecore/select?q={!mtas_cql+field="mtas_text"+query="<s/>+containing+[t_lc=\"de\"]"}`
  - `http://localhost:8087/solr/vrecore/select?q={!mtas_cql+field="mtas_text"+query="<s/>+containing+[t_lc=\"de\"]"}&rows=0&mtas=true&mtas.termvector=true&mtas.termvector.0.key=frequentielijst&mtas.termvector.0.field=mtas_text&mtas.termvector.0.type=n,sum&mtas.termvector.0.prefix=t_lc&mtas.termvector.0.number=10&mtas.termvector.0.sort.type=n&&mtas.termvector.0.sort.direction=desc`

30. [ ] Create a CMD record via CMDI Forms
31. [ ] Upload the CMD record and associate it with the output file
32. [ ] Deposit the SIP into TLA-FLAT
33. [ ] Show the AIP in TLA-FLAT

99. [X] Stop the VRE
```sh
./stop-vre.sh
```
99. [X] Stop LaMachine
```
vagrant halt
```
