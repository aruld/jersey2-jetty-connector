Jersey 2 JAX-RS Client Connector for Jetty
===============================

Implemented based on Jersey 2 m12 release. It supports both synchronous and asynchronous client implementation.

It supports cookie handling, basic authentication and proxy servers.

Setup
-----

1. git clone https://github.com/aruld/jersey2-jetty-connector.git
2. mvn package
3. Include this in your POM.

    <dependency>
        <groupId>org.glassfish.jersey.connectors</groupId>
        <artifactId>jersey-jetty-connector</artifactId>
        <version>2.0-m12</version>
    </dependency>



Usage
-----

    ClientConfig cc = new ClientConfig();
    cc.connector(new JettyConnector(clientConfig));
    Client client = ClientFactory.newClient(cc);

    WebTarget target = client.target("http://localhost:8080");

    // Perform GET
    Response response = target.path("test").request().get();
    String entity = response.readEntity(String.class);
    System.out.println(entity);

    // Perform async GET
    response = target.path("test").request().async().get().get();
    entity = response.readEntity(String.class);
    System.out.println(entity);

    // Shutdown the client
    client.close();

