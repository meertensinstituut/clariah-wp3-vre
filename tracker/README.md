# Tracker
Creates provenance using kafka logs.

Uses:
- Kafka
- KSQL

# Use KSQL CLI:
```
docker exec -ti vre_ksql-cli_1 ksql http://ksql-server:8088
```

Test:
```
curl -X "POST" "http://localhost:8090/ksql" \
     -H "Content-Type: application/vnd.ksql.v1+json; charset=utf-8" \
     -d $'{
  "ksql": "LIST TOPICS;",
  "streamsProperties": {}
}'
```

Bronnen:
- https://docs.confluent.io/current/ksql/docs/tutorials/basics-docker.html
- https://docs.confluent.io/current/ksql/docs/developer-guide/api.html