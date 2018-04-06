# Switchboard

Prepare and coordinate service requests.

Tools, languages and techniques:
 - Java 9
 - Tomcat 9
 - Jersey

## Workflow
- A service deployment request contains a list of input files.
- Switchboard locks the input files in owncloud, and creates links to these files for a deployment container in `<workDir>/input/`.
- After a stopped or finished deployment links are removed, files are unlocked and files in `<workDir>/output/` are moved to owncloud in an output folder which is returned in json result when polling.

## Endpoints
- `POST /switchboard/rest/exec/<service>`: deploy new service  
- `GET  /switchboard/rest/exec/task/<workDir>`: poll status of deployment

## Deployment
- Run: `./start-switchboard.sh`. Runs at: `localhost:9010/switchboard`
- Request service: POST `localhost:9010/switchboard/rest/exec/<service>` with json body containing info needed by service.

Input json-format:
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

Output in `config.json`:
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

## Example
**1. Start deployment of TEST service**
```
curl -X POST http://localhost:9010/switchboard/rest/exec/TEST -H "Content-Type: application/json" -d '{ "params": [ { "name": "foo", "type": "file", "value": 1, "params": [ { "language": "nld" } ] } ] }'
```
...which results in: 
```
{
  "msg":"Deployment of service [TEST] has been requested.",
  "workDir":"ouzirghg",
  "status":"DEPLOYED"
}
```
**2. Check status of deployment**

Enter in console:
```
curl http://localhost:9010/switchboard/rest/exec/task/<workDir>
```
...which (when finished) results in: 
```
{
  "status":"FINISHED",
  "msg":"Task finished, clean up the queue.",
  "outputDir":"admin/files/output-2018-03-28_15-19-54-870/",
  "workDir":"ouzirghg"
}
```

**3. Check results**

Open owncloud in your browser to view result files in <outputDir>.

Or enter in console:
```
curl 'http://localhost:8082/remote.php/webdav/<outputDir>/result.txt
```
...which returns the contents of the file.

