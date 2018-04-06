Clariah VRE - Registry
===

- Run `docker-compose up`
- DreamFactory admin access: [http://localhost:8089/](http://localhost:8089/) with `ADMIN_EMAIL` and `ADMIN_PASSWORD` form [environment.dreamfactory](environment.dreamfactory) file

**Access Services Registry**

- define `USER_SERVICES_EMAIL` and `USER_SERVICES_PASSWORD` from [environment.dreamfactory](environment.dreamfactory) file
- get token for session: `servicestoken=$(curl -s -k -3 -X POST "http://localhost:8089/api/v2/user/session" -d '{ "email" : "'${USER_SERVICES_EMAIL}'", "password" : "'${USER_SERVICES_PASSWORD}'" }' -H "Content-Type: application/json" | jq -r .session_token)`
- define `APP_KEY_SERVICES` from [environment.dreamfactory](environment.dreamfactory) file
- get tables: `curl -X GET "http://localhost:8089/api/v2/services/_table" -H  "accept: application/json" -H "X-DreamFactory-Api-Key: ${APP_KEY_SERVICES}" -H  "X-DreamFactory-Session-Token: ${servicestoken}"`

*changed 23-03-2018: default no X-DreamFactory-Session-Token necessary*

**Access Objects Registry**

- define `USER_OBJECTS_EMAIL` and `USER_OBJECTS_PASSWORD` from [environment.dreamfactory](environment.dreamfactory) file
- get token for session: `objectstoken=$(curl -s -k -3 -X POST "http://localhost:8089/api/v2/user/session" -d '{ "email" : "'${USER_OBJECTS_EMAIL}'", "password" : "'${USER_OBJECTS_PASSWORD}'" }' -H "Content-Type: application/json" | jq -r .session_token)`
- define `APP_KEY_OBJECTS` from [environment.dreamfactory](environment.dreamfactory) file
- get tables: `curl -X GET "http://localhost:8089/api/v2/objects/_table" -H  "accept: application/json" -H "X-DreamFactory-Api-Key: ${APP_KEY_OBJECTS}" -H  "X-DreamFactory-Session-Token: ${objectstoken}"`

*changed 23-03-2018: default no X-DreamFactory-Session-Token necessary*





