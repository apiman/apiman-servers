# API Gateway (Elasticsearch)

## Description
This is a microservice for the apiman API Gateway using Elasticsearch as the provider for all
major components (registry, rate limiting, metrics, etc).

## Usage
Make a copy of the properties file found at `src/main/resources`:

```
src/main/resources/gateway_es-apiman.properties-SAMPLE
```

Call the copy of this file `gateway_es-apiman.properties` and make any necessary
changes to it that you desire.  Primarily the changes will be related to the location
of your elasticsearch server.

Now just run the `Starter` class either from the command line using maven or by importing
this project into your IDE and running it from there.

### Command line
```
mvn exec:java -Dexec.mainClass="io.apiman.servers.gateway_es.Starter"
```

### From IDE
We leave this as an exercise to the reader. :)
