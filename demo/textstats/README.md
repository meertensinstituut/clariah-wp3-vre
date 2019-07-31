# Textstats

Textstats basic statistics and keyword/sentence extraction for TEI-encoded documents. For more information see: https://github.com/proprefenetre/textstats

## Create Recipe

### Docker-compose

At the moment the VRE has no mechanism to start contains on the fly. Therefore we will add textstats to `docker-compose.yml`:

```
  textstats:
    image: eigenraam/textstats
```

Because deployment-service needs to know where tika lives, we add tika to its dependencies:
```
  deployment:
    depends_on:
      - textstats
```

### Java class
See: `Textstats.java` in `../deployment-service`

