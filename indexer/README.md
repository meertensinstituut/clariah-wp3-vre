Indexer
===

Put file into Solr index

Deployment
---
- Run `mvn clean install`
- Run `./start-indexer.sh`.


### Test Kafka
Open bash in container: `./indexer-bash.sh`. In seperate terminals:

Run **example consumer of nextcloud topic**:
```
java -jar --add-modules java.xml.bind ./target/indexer-0.1-SNAPSHOT.jar test-consume-nextcloud
```

Run **example consumer of indexer topic**:
```
java -jar --add-modules java.xml.bind ./target/indexer-0.1-SNAPSHOT.jar test-consume-indexer
```

Run **example producer** in container:
```
java -jar --add-modules java.xml.bind ./target/indexer-0.1-SNAPSHOT.jar produce
```

Run **consumer** in container:
```
java -jar --add-modules java.xml.bind ./target/indexer-0.1-SNAPSHOT.jar consume
```

