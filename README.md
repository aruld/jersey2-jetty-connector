[![Build Status](https://travis-ci.org/aruld/jersey2-jetty-connector.png)](https://travis-ci.org/aruld/jersey2-jetty-connector)

Jersey 2 JAX-RS Client Connector for Jetty
===============================

Implemented based on Jersey 2 m12 release. It supports both synchronous and asynchronous client invocation.

It supports cookie handling, basic authentication and proxy servers.

Requirements
----

The project requires JDK 7, since Jetty 9 required JDK 7.

Setup
-----

1. git clone https://github.com/aruld/jersey2-jetty-connector.git
2. mvn package
3. Include this in your Maven POM.

```xml
    <dependency>
        <groupId>org.glassfish.jersey.connectors</groupId>
        <artifactId>jersey-jetty-connector</artifactId>
        <version>2.0-m12</version>
    </dependency>
```


Usage
-----

    ClientConfig cc = new ClientConfig();
    cc.connector(new JettyConnector(clientConfig));
    cc.register(new LoggingFilter(LOGGER, true));
    Client client = ClientFactory.newClient(cc);

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

    Feb 10, 2013 3:36:31 PM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 1 * LoggingFilter - Request received on thread Grizzly(1)
    1 > GET http://localhost:8080/test
    1 > user-agent: Jersey/2.0-m12 (Jetty/9.0.0.RC0)
    1 > host: localhost:8080
    1 > accept-encoding: gzip

    Feb 10, 2013 3:36:33 PM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 2 * LoggingFilter - Response received on thread Grizzly(1)
    2 < 200
    2 < Content-Type: text/plain
    GET

    Feb 10, 2013 3:36:33 PM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 3 * LoggingFilter - Request received on thread Grizzly(2)
    3 > GET http://localhost:8080/test
    3 > user-agent: Jersey/2.0-m12 (Jetty/9.0.0.RC0)
    3 > host: localhost:8080
    3 > accept-encoding: gzip

    Feb 10, 2013 3:36:35 PM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 4 * LoggingFilter - Response received on thread Grizzly(2)
    4 < 200
    4 < Content-Type: text/plain
    GET


Client-side Logging
------

    2013-02-10 15:36:31.148:INFO:oejc.HttpClient:main: Started org.eclipse.jetty.client.HttpClient@60dd1773
    Feb 10, 2013 3:36:31 PM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 1 * LoggingFilter - Request received on thread main
    1 > GET http://localhost:8080/test

    Feb 10, 2013 3:36:33 PM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 2 * LoggingFilter - Response received on thread main
    2 < 200
    2 < Content-Type: text/plain
    2 < Date: Mon, 11 Feb 2013 01:36:33 GMT
    2 < Content-Length: 3
    GET

    GET
    Feb 10, 2013 3:36:33 PM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 3 * LoggingFilter - Request received on thread jersey-client-async-executor-0
    3 > GET http://localhost:8080/test

    GET
    Feb 10, 2013 3:36:35 PM org.glassfish.jersey.filter.LoggingFilter log
    INFO: 4 * LoggingFilter - Response received on thread HttpClient@1625102195-14
    4 < 200
    4 < Content-Type: text/plain
    4 < Date: Mon, 11 Feb 2013 01:36:35 GMT
    4 < Content-Length: 3
    GET

    2013-02-10 15:36:35.710:INFO:oejc.HttpClient:main: Stopped org.eclipse.jetty.client.HttpClient@60dd1773

