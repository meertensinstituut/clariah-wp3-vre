# CLARIAH - WP3 - VRE - demo

## Prepare the demo

0. Insert service recipes: 
```sh
./VRE-demo-init.sh
```

## Scenario in the frontend

0. Login into NextCloud
   - http://localhost:8082/
   - (login `test:achtkarakters`)

![Login into NextCloud](screens/login-into-nextcloud.png)

0. Upload a plain text file

![Upload a plain text file](screens/upload-text-file.png)

0. Go to the VRE
   - http://localhost:3000/
   - switch to the Files tab

![The plain text file in the VRE](screens/text-file-in-vre.png)

0. View the file

![View the text file](screens/view-text-file.png)

0. Execute UCTO on the file

![Select UCTO on the text file](screens/select-ucto-service.png)

![Specify the UCTO parameters](screens/set-ucto-parameters.png)

![View for UCTO to complete](screens/wait-for-ucto-to-complete.png)



## Scenario on the backend

0. Create a sample text file
  - create a fresh `nos.txt` with a news item from nos.nl

0. Upload the sample text file

```sh
./VRE-demo-upload.sh
```

At the end of the script the _id_ of the new file is printed. Remember it for the next step.

0. Execute UCT for the sample text file

```sh
./VRE-demo-exec.sh <id>
```

Replace `<id>` by the _id_ assigned to the file during upload.

## Inspect the backend

0. [X] Show the files in NextCloud

  - http://localhost:8082/
  - (login `test:achtkarakters`)
  
0. [X] Show the messages in Kafka
  
  - goto http://localhost:9000/#/observe

0. [X] Show the entries in the Object Registry

  - goto http://localhost:8089
  - login `noreply@dreamfactory.dev:password`
  - goto `Data > Objects VRE > Object`
  - click `Set Service`
  - enable all fields in the `Fields` tab

0. [X] Show the entries in the Service Registry

  - goto http://localhost:8089
  - login `noreply@dreamfactory.dev:password`
  - goto `Data > Services VRE > Service`
  - click `Set Service`
  - enable all fields in the `Fields` tab


