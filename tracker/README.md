# Tracker
Creates provenance using kafka logs.

Uses:
- Kafka
- KSQL

# Use KSQL CLI:
- Run: `./start-ksql-cli.sh`
- In cli: `run script /scripts/create-table.ksql`

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
- https://github.com/confluentinc/demo-scene/blob/master/community-components-only/docker-compose.yml

TODO:
- add object-ids used in deployment request to switchboard logging
- link topics with object ids:
  - vre_tagger_topic.$.object
  - vre_recognizer_topic.$.objectId
  - vre_indexer_topic.$.object_id
  - vre_switchboard_topic.$.???
