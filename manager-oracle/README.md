# API Manager (JPA w/ Oracle 12)

## Description
This is a microservice for the apiman API Manager that stores data in a Oracle 12 
database via JPA.  It creates a Hikari datasource to your Oracle 12 database, and
binds it to JNDI so that apiman can use it.

Additionally this microservice will use elasticsearch (by default) for metrics
data.

## Usage
Make a copy of all `*-SAMPLE` files found in `src/main/resources`.  Use
the appropriate names by dropping the `-SAMPLE` from the filename.  You should
end up with this:

```
$ pwd
/home/user/git/apiman/apiman-servers/manager-oracle/src/main/resources
$ ls -al
total 7
drwxr-xr-x    1 user     user     4096 Jul 20 11:07 .
drwxr-xr-x    1 user     user        0 Jul 20 08:25 ..
-rw-r--r--    1 user     user       64 Jul 21 08:00 .gitignore
-rw-r--r--    1 user     user      184 Jul 20 10:32 hikari.properties
-rw-r--r--    1 user     user      180 Jul 20 10:32 hikari.properties-SAMPLE
-rw-r--r--    1 user     user     1238 Jul 21 09:19 manager_oracle-apiman.properties
-rw-r--r--    1 user     user     1237 Jul 20 11:08 manager_oracle-apiman.properties-SAMPLE
-rw-r--r--    1 user     user       62 Jul 20 10:39 users.list
-rw-r--r--    1 user     user       60 Jul 20 10:38 users.list-SAMPLE
```

Now edit each of the `*.properties` files, if necessary, to tailor the 
microservice to your environment.  In particular you will likely want to modify
the database connection settings in `hikari.properties`.

Now just run the `Starter` class either from the command line using maven or by importing
this project into your IDE and running it from there.

### Command line
```
mvn exec:java -Dexec.mainClass="io.apiman.servers.manager_oracle.Starter"
```

### From the built artifact
Once built and packaged via maven, you can run the resulting artifact as an executable JAR:

```
java -jar apiman-manager-oracle.jar
```

### From IDE
We leave this as an exercise to the reader. :)

### Oracle 12 Driver
Note that you cannot run this server configuration without downloading the Oracle 12 JDBC
driver and installing it in your .m2 directory.  To do this:

1. Download the oracle driver:  http://www.oracle.com/technetwork/database/features/jdbc/index-091264.html
2. Install it:  
```
mvn install:install-file -Dfile=ojdbc7.jar -DgroupId=com.oracle -DartifactId=ojdbc7 -Dversion=12.1.0.2 -Dpackaging=jar
```
