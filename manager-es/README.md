# API Manager (Elasticsearch)

## Description
This is a microservice for the apiman API Manager that stores data in an 
elasticsearch instance.  The elasticsearch instance must be running externally
in some location (by default on localhost:9200).

## Usage
Make a copy of all `*-SAMPLE` files found in `src/main/resources`.  Use
the appropriate names by dropping the `-SAMPLE` from the filename.  You should
end up with this:

```
$ pwd
/home/user/git/apiman/apiman-servers/manager-es/src/main/resources
$ ls -al
total 7
drwxr-xr-x    1 user     user     4096 Jul 20 11:07 .
drwxr-xr-x    1 user     user        0 Jul 20 08:25 ..
-rw-r--r--    1 user     user       64 Jul 21 08:00 .gitignore
-rw-r--r--    1 user     user     1238 Jul 21 09:19 manager_es-apiman.properties
-rw-r--r--    1 user     user     1237 Jul 20 11:08 manager_es-apiman.properties-SAMPLE
-rw-r--r--    1 user     user       62 Jul 20 10:39 users.list
-rw-r--r--    1 user     user       60 Jul 20 10:38 users.list-SAMPLE
```

Now edit each of the `*.properties` files, if necessary, to tailor the 
microservice to your environment.  In particular you will likely want to modify
the location of the elasticsearch instance.

Now just run the `Starter` class either from the command line using maven or by importing
this project into your IDE and running it from there.

### Command line
```
mvn exec:java -Dexec.mainClass="io.apiman.servers.manager_es.Starter"
```

### From IDE
We leave this as an exercise to the reader. :)
