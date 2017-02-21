# API Gateway (File Registry)

## Description
This is a microservice for the apiman API Gateway that loads its configuration from a JSON file.

## Prerequisites
Create a JSON file containing the clients and APIs you want to load at startup. For example:

```json
{
     "apis": [{
        "publicAPI": true,
        "organizationId": "foo",
        "apiId": "foo",
        "version": "foo",
        "endpoint": "http://www.example.org/foo/bar",
        "endpointType": "rest",
        "endpointContentType": "",
        "endpointProperties": {},
        "parsePayload": false,
        "apiPolicies": [{
            "policyJsonConfig": "",
            "policyImpl": "plugin:io.apiman.plugins:apiman-plugins-test-policy:1.2.9-SNAPSHOT:war/io.apiman.plugins.test_policy.TestPolicy"
        }]
    }],
    "clients": [{
        "organizationId": "foo",
        "clientId": "fooClient",
        "version": "foo",
        "apiKey": "12345",
        "contracts": [{
            "apiOrgId": "foo",
            "apiId": "foo",
            "apiVersion": "foo",
            "plan": "foo",
            "policies": []
        }]
    }]
}
```

## Usage

* Make a copy of the properties file found at `src/main/resources`:

```
src/main/resources/gateway_file-apiman.properties-SAMPLE
```

Call the copy of this file `gateway_file-apiman.properties` and make any necessary
changes to it that you desire.

* Set `apiman-gateway.registry.configUri` to point to your JSON config. This can be a local `file://` or HTTP URI.

### Command line
```
mvn exec:java -Dexec.mainClass="io.apiman.servers.gateway_file.Starter"
```

### From the built artifact
Once built and packaged via maven, you can run the resulting artifact as an executable JAR:

```
java -jar apiman-gateway-file.jar
```

### From IDE
We leave this as an exercise to the reader. :)
