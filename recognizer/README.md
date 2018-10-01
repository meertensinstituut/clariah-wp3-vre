Recognizer
===

Determine file type using FITS

Development
---
- Use java 9.
- Run `mvn clean install` to generate sources.

Deployment
---
- Run `./start-recognizer.sh`.

**Note**: when building of fits image fails because of downloading `fits-1.2.0.zip`: place a local copy in `./fit-zip/fits/1.2.0.zip`.

### Test Kafka
Open bash in container: `./recognizer-bash.sh`. In seperate terminals:

Run **example consumer of nextcloud topic**:
```
java -jar --add-modules java.xml.bind ./target/recognizer-0.1-SNAPSHOT.jar test-consume-nextcloud
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

