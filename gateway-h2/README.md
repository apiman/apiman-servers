# API Gateway (H2 Registry)

## Description
This is a microservice for the apiman API Gateway that uses an H2 database as the registry
implementation, along with (typically) Elasticsearch for the other major components (rate 
limiting, metrics, etc).

## Requirements
You should probably have an instance of Elasticsearch already running, and it must version 1.x (for 
example, 1.7.2).  We do not yet support ES 2.x.

Download ES 1.7.2 here:  https://www.elastic.co/downloads/past-releases/elasticsearch-1-7-2

## Usage
Make a copy of the properties file found at `src/main/resources`:

```
src/main/resources/gateway_h2-apiman.properties-SAMPLE
```

Call the copy of this file `gateway_h2-apiman.properties` and make any necessary
changes to it that you desire.

Next, make a copy of the hikari properties file found in the same folder:

```
src/main/resources/hikari.properties-SAMPLE
```

Call the copy of this file `hikari.properties` and make any necessary
changes to it that you desire.  In particular, it's usually necessary to change the
file path to the H2 data directory.

Now just run the `Starter` class either from the command line using maven or by importing
this project into your IDE and running it from there.

### Command line
```
mvn exec:java -Dexec.mainClass="io.apiman.servers.gateway_h2.Starter"
```

### From the built artifact
Once built and packaged via maven, you can run the resulting artifact as an executable JAR:

```
java -jar apiman-gateway-h2.jar
```

### From IDE
We leave this as an exercise to the reader. :)
