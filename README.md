[![Build Status](https://travis-ci.org/aruld/jersey2-jetty-connector.png)](https://travis-ci.org/aruld/jersey2-jetty-connector)
[![Build Status](https://buildhive.cloudbees.com/job/aruld/job/jersey2-jetty-connector/org.glassfish.jersey.connectors$jersey-jetty-connector/badge/icon)](https://buildhive.cloudbees.com/job/aruld/job/jersey2-jetty-connector/org.glassfish.jersey.connectors$jersey-jetty-connector/)

Jersey 2 JAX-RS Client Connector for Jetty
===============================

Jetty connector is based on Jersey 2 JAX-RS Client framework. It implements support for synchronous and
asynchronous client invocation using Jetty 9 Fluent API.

Features include cookie handling, basic authentication and proxy server support.

Supported HTTP verbs: GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE, CONNECT, MOVE

Requirements
----

The project requires JDK 7, since Jetty 9 required JDK 7.

Setup
-----

0. Non-maven projects can download the snapshot jar from [buildhive](https://buildhive.cloudbees.com/job/aruld/job/jersey2-jetty-connector/org.glassfish.jersey.connectors$jersey-jetty-connector/lastSuccessfulBuild/artifact/).
1. git clone https://github.com/aruld/jersey2-jetty-connector.git
2. mvn package
3. Include this in your Maven POM.

```xml
    <dependency>
        <groupId>org.glassfish.jersey.connectors</groupId>
        <artifactId>jersey-jetty-connector</artifactId>
        <version>2.0-SNAPSHOT</version>
    </dependency>
```


Usage
-----

    ClientConfig cc = new ClientConfig();
    cc.connector(new JettyConnector(clientConfig));
    cc.register(new LoggingFilter());
    Client client = ClientBuilder.newClient(cc);

    WebTarget target = client.target("http://localhost:8080");

    // Perform GET
    Response response = target.path("test").request().get();
    String entity = response.readEntity(String.class);
    System.out.println(entity);

    // Perform async GET
    Future<Response> future = target.path("test").request().async().get();
    response = future.get(3, TimeUnit.SECONDS);
    entity = response.readEntity(String.class);
    System.out.println(entity);

    // Shutdown the client
    client.close();


Server-side Logging
------

    Feb 17, 2013 11:27:35 AM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 1 * LoggingFilter - Request received on thread Grizzly-worker(1)
    1 > GET http://localhost:8080/test
    1 > user-agent: Jersey/2.0-m12-1 (Jetty/9.0.0-SNAPSHOT)
    1 > host: localhost:8080
    1 > accept-encoding: gzip

    Feb 17, 2013 11:27:37 AM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 2 * LoggingFilter - Response received on thread Grizzly-worker(1)
    2 < 200
    2 < Content-Type: text/plain
    GET

    Feb 17, 2013 11:27:37 AM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 3 * LoggingFilter - Request received on thread Grizzly-worker(2)
    3 > GET http://localhost:8080/test
    3 > user-agent: Jersey/2.0-m12-1 (Jetty/9.0.0-SNAPSHOT)
    3 > host: localhost:8080
    3 > accept-encoding: gzip

    Feb 17, 2013 11:27:39 AM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 4 * LoggingFilter - Response received on thread Grizzly-worker(2)
    4 < 200
    4 < Content-Type: text/plain
    GET

Client-side Logging
------

    2013-02-17 11:27:34.570:INFO:oejc.HttpClient:main: Started org.eclipse.jetty.client.HttpClient@789caeb2
    Feb 17, 2013 11:27:34 AM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 1 * LoggingFilter - Request received on thread main
    1 > GET http://localhost:8080/test

    Feb 17, 2013 11:27:37 AM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 2 * LoggingFilter - Response received on thread main
    2 < 200
    2 < Content-Type: text/plain
    2 < Date: Sun, 17 Feb 2013 21:27:37 GMT
    2 < Content-Length: 3

    GET
    Feb 17, 2013 11:27:37 AM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 3 * LoggingFilter - Request received on thread jersey-client-async-executor-0
    3 > GET http://localhost:8080/test

    GET
    Feb 17, 2013 11:27:39 AM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 4 * LoggingFilter - Response received on thread HttpClient@2023534258-18
    4 < 200
    4 < Content-Type: text/plain
    4 < Date: Sun, 17 Feb 2013 21:27:39 GMT
    4 < Content-Length: 3

    2013-02-17 11:27:39.096:INFO:oejc.HttpClient:main: Stopped org.eclipse.jetty.client.HttpClient@789caeb2


TODO
----

* Support Read timeout/connect timeout
* AppClient sample takes longer to shutdown async client (Jersey bug?)
* SSL support
* Add more tests
