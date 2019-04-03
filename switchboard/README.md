# Switchboard

Prepare and coordinate service requests.

Tools, languages and techniques:
 - Java 11
 - Tomcat
 - Jersey

# Development
- Add static files (e.g. test users) to `./static_files`

## Local deployment

- Run: `./start-switchboard.sh`. Runs at: `localhost:9010/switchboard`

- Redeploy: `docker exec vre_switchboard_1 bash -c "../tomcat/bin/shutdown.sh && ./docker/docker-run-switchboard.sh"`

- To make the tests run on a developer host machine, run like:
```
export USER_TO_LOCK_WITH=$USER
export USER_TO_UNLOCK_WITH=$USER
mvn clean test
```
Note 1: in production locker and unlocker should be different users, as defined in docker-compose.yml

Note 2: make sure port 9998 (jerseytest) and 1080 (mockserver) are free

## Testing

Run: `docker-compose build switchboard`

Note 1: environment variables are mocked with surefire plugin, see pom.xml
Note 2: in order to run tests `$USER` should be set with existing username

## Workflow
- A service deployment request contains a list of input files.
- Switchboard locks the input files in nextcloud, and creates links to these files for a deployment container in `<workDir>/input/`.
- After a stopped or finished deployment links are removed, files are unlocked and files in `<workDir>/output/` are moved to nextcloud in an output folder which is returned in json result when polling.

## Endpoints
Endpoints are prefixed with `/switchboard/rest`

- `GET /health`
Check if switchboard is up.

- `POST /exec/{service}`: 
Deploy new service  

- `GET /exec/task/{workDir}`: 
Get status of deployment

- `GET /objects/{objectId}/services`:
Get services matching profile of object (atm only based on mimetype)

- `GET /params/{serviceId}`:
Get services matching profile of object (atm only based on mimetype)

## How to request a deployment with switchboard
`POST localhost:9010/switchboard/rest/exec/<service>` with json body containing info needed by service.

Example format of json body:
```
{
  "params": [
    {
      "name": "untokinput",
      "type": "file",
      "value": 1,
      "params": [
        {
          "language": "nld",
          "author": "J. Jansen"
        }
      ]
    }
  ]
}
```

The value of `value` should refer to an ID in the object registry. Switchboard converts this ID to a file path that is available to the requested service.
Switchboard replaces these ids with file paths.
The converted json is saved in a file called `config.json` in which deployment-service looks for parameters to deploy the requested service.

Example format of `config.json`:
```
{
  "params": [
    {
      "name": "untokinput",
      "type": "file",
      "value": "path/to/test.txt",
      "params": [
        {
          "language": "nld",
          "author": "J. Jansen"
        }
      ]
    }
  ]
}
```

## Deployment status codes:
- DEPLOYED        (201 - Created) 
- RUNNING         (202 - Accepted) 
- FINISHED        (200 - OK)
- NOT_FOUND       (404 - Not Found)
- ALREADY_RUNNING (403 - Forbidden) 
