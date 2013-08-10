[![Build Status](https://travis-ci.org/aruld/jersey2-jetty-connector.png)](https://travis-ci.org/aruld/jersey2-jetty-connector)
[![Build Status](https://buildhive.cloudbees.com/job/aruld/job/jersey2-jetty-connector/org.glassfish.jersey.connectors$jersey-jetty-connector/badge/icon)](https://buildhive.cloudbees.com/job/aruld/job/jersey2-jetty-connector/org.glassfish.jersey.connectors$jersey-jetty-connector/)

Jersey 2 JAX-RS Client Connector for Jetty
===============================

Jetty connector is based on Jersey 2 JAX-RS Client framework. It implements support for synchronous and
asynchronous client invocation using Jetty 9 Fluent [Client](http://www.eclipse.org/jetty/documentation/current/clients.html) API.

Features include cookie handling, basic authentication, proxy server support, global redirects, timeouts and SSL.

Supported HTTP verbs: GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE, CONNECT, MOVE

Requirements
----

JDK 7 is required to run Jetty 9 connector.

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
        <version>2.1</version>
    </dependency>
```


Basic Usage
-----

    ClientConfig cc = new ClientConfig();
    cc.connector(new JettyConnector(cc));
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


Basic Auth
-----

    ClientConfig cc = new ClientConfig();
    cc.property(JettyClientProperties.BASIC_AUTH, new BasicAuthentication(getBaseUri(), "WallyWorld", "name", "password"));
    cc.connector(new JettyConnector(cc));
    Client client = ClientBuilder.newClient(cc);

SSL
---

    SslConfigurator sslConfig = SslConfigurator.newInstance()
            .trustStoreBytes(ByteStreams.toByteArray(trustStore))
            .trustStorePassword("asdfgh")
            .keyStoreBytes(ByteStreams.toByteArray(keyStore))
            .keyPassword("asdfgh");

    ClientConfig cc = new ClientConfig();
    cc.property(JettyClientProperties.SSL_CONFIG, sslConfig);
    cc.connector(new JettyConnector(cc));
    Client client = ClientBuilder.newClient(cc);

Cookie Handling
-------

    ClientConfig cc = new ClientConfig();
    cc.property(JettyClientProperties.DISABLE_COOKIES, true);//ignore all cookies
    Client client = ClientBuilder.newClient(cc.connector(new JettyConnector(cc.getConfiguration())));

Redirects
------

    ClientConfig cc = new ClientConfig().property(JettyClientProperties.FOLLOW_REDIRECTS, true);//global redirect
    cc.connector(new JettyConnector(cc));
    Client c = ClientBuilder.newClient(cc);
    WebTarget t = c.target(u);
    t.property(ClientProperties.FOLLOW_REDIRECTS, false);//per-request override

Timeouts
------

    ClientConfig cc = new ClientConfig().property(ClientProperties.READ_TIMEOUT, 1000).property(ClientProperties.CONNECT_TIMEOUT, 1000);
    cc.connector(new JettyConnector(cc));
    Client c = ClientBuilder.newClient(cc);

Check out tests for more usage!