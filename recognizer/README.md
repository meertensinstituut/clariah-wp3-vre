Recognizer
===

Determine file type using FITS

Deployment
---
- Run `./start-recognizer.sh`.

**Note**: when building of fits image fails because of downloading `fits-1.2.0.zip`: place a local copy in `./fit-zip/fits/1.2.0.zip`.

### Test Kafka
Open bash in container: `./recognizer-bash.sh`. In seperate terminals:

Run **example consumer of owncloud topic**:
```
java -jar --add-modules java.xml.bind ./target/recognizer-0.1-SNAPSHOT.jar test-consume-owncloud
```

Run **example consumer of recognizer topic**:
```
java -jar --add-modules java.xml.bind ./target/recognizer-0.1-SNAPSHOT.jar test-consume-recognizer
```

Run **example producer** in container:
```
java -jar --add-modules java.xml.bind ./target/recognizer-0.1-SNAPSHOT.jar produce
```

Run **consumer** in container:
```
java -jar --add-modules java.xml.bind ./target/recognizer-0.1-SNAPSHOT.jar consume
```

