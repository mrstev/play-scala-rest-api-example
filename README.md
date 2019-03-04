# Play REST API - Bill of Materials test project



This application has been forked from the example project for [Making a REST API in Play](http://developer.lightbend.com/guides/play-rest-api/index.html).

I committed my changes in a single commit to allow one to inspect the sum total of the files that I worked on.

There were a couple of improvements that I made to the project that new new features I had not integrated with a Play project before:
- I used an asynchronous MySQL database library. This allows the threads connecting to the database to sleep while waiting for replies.  With the entire Web service operating asynchronously from front to back, web this service can handle a high number of concurrent requests requiring database (or other remote resource) access while causing relatively little thread contention, leading to higher throughput.

- I experimented for the first time with the Quill library (https://getquill.io/) for creating compile-time query generation and validation for all MySQL queries. Quill uses an abstract syntax tree to represent SQL using the functional constructs of Scala and validates these generated SQL expressions using Type Inference and constraints. 
Queries can be written for a variety of target data stores in native Scala.

## Running

The quickest way to get the application running is to download the assembled [.jar file](https://github.com/mrstev/play-scala-rest-api-example/blob/2.7.x/bill-of-materials-rest-api-assembly-1.0-SNAPSHOT.jar) file in the top level directory and run it using the following command:

```bash
java -jar /bill-of-materials-rest-api-assembly-1.0-SNAPSHOT.jar
```
WARNING: It is a very large file.  I did not remove any of the unused dependencies in the example project I forked from, so there are *many* packages in there that don't need to be.

Play will start up on the HTTP port at <http://localhost:9000/>. 

####Database Config

The connection to the MySQL Database is reference in the file at `/conf/application.conf`. Within it the database is declared in two places (one for schema evolutions, and one for quill's usage). If the configuration is needed to be changed before running the above jar, one can copy the `application.conf` file, change the settings, and launch the application pointing to the new conf file:
```bash

java -jar -Dconfig.file=/path/to/conf/application.conf /path/to/bill-of-materials-rest-api-assembly-1.0-SNAPSHOT.jar
``` 

Mor information about how to configure the application is available [here](https://www.playframework.com/documentation/2.7.x/Deploying) and [here](https://www.playframework.com/documentation/2.7.x/ProductionConfiguration)

####Using SBT to Edit and Run in Dev Mode

If you have sbt, you can clone this project an run using sbt. The following at the command prompt will start up Play in development mode:

```bash
sbt run
```

Play will again start up on the HTTP port at <http://localhost:9000/>.   You don't need to deploy or reload anything -- changing any source code while the server is running will automatically recompile and hot-reload the application on the next HTTP request. 

### Usage


Please take a look at the Postman documentation [Here](https://documenter.getpostman.com/view/6751411/S11LtHhy) for the published API, and [Here](https://www.getpostman.com/collections/f2754eaa58c38bd441db) for a collection of Postman contract tests. 
