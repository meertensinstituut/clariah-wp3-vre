# CLARIAH - WP3 - VRE - demo

0. Insert service recipes: 
```sh
./VRE-demo-init.sh
```

0. [X] Upload a sample text file
  - create a fresh one with a news item from nos.nl
```sh
curl -v 'http://localhost:8082/remote.php/webdav/nos.txt' \
     -X PUT \
     -H 'Content-Type: text/plain; charset=UTF-8' \
     -u test:achtkarakters \
     -d '@nos.txt'
```

0. [X] Show the file in NextCloud
  
0. [X] Show the message in Kafka
  
0. [X] Show the entry in the Object Registry

0. [X] Show the entries in the Service Registry

0. [X] Deploy `ucto` in UI

0. [X] Show the output in OwnCloud
   - http://localhost:8082/
   - (login `test:achtkarakters`)

