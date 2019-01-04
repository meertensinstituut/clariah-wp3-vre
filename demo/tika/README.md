# From Tika to Recipe

*Using Apache Tika to scrape text from an html file -- a manual*

## Steps
0. Add apache tika container to to vre container set-up
0. Create cmdi file that describes with which parameters to call tika
0. Create java file to call tika container

## Add Tika to VRE
Luckily Tika has already been dockerized by [Locicalspark](https://hub.docker.com/r/logicalspark/docker-tikaserver). We will use this image.

To scrape text from html with tika we can simply start the container and use the following curl command:
```
docker run -d -p 9998:9998 logicalspark/docker-tikaserver
curl -T test.html http://localhost:9998/tika/main --header "Accept: text/plain"
```

At the moment all our containers are defined in `./docker-compose.yml`.
So to use Tika in the VRE we add a new docker entry there:
```
  tika:
    image: logicalspark/docker-tikaserver:1.20
```

Since deployment-service needs to know where tika lives, we add tika to its dependencies:
```
  deployment:
    ...
    depends_on:
      ...
      - tika
```

## Create cmdi file 
Tika only needs to know the filename; the result will be returned as plain text.
In our cmdi file will therefore define only one parameter, an input file. See `tika.cmdi`.

Now we need to insert our cmdi as a recipe in the service-table of the services-database. See: `tika.sql` and `tika.sh`

## Create java file
The deployement service communicates with tika. 
It should know how to interpret the tika cmdi file and how to invoke the tika service in its docker container.
Therefore we create a new java class. See: `../../deployment-service/deployment-lib/src/main/java/nl/knaw/meertens/deployment/lib/recipe/Tika.java`

## Using tika
Upload an example html-file to nextcloud, open VRE-UI and scrape the file by processing it with the TIKA-service.
