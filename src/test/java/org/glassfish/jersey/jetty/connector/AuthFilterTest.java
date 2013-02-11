package org.glassfish.jersey.jetty.connector;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.filter.HttpBasicAuthFilter;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public class AuthFilterTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(AuthFilterTest.class.getName());

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(AuthTest.AuthResource.class);
        config.register(new LoggingFilter(LOGGER, true));
        return config;
    }


    @Override
    protected void configureClient(ClientConfig clientConfig) {
        clientConfig.connector(new JettyConnector(clientConfig));
    }

    @Test
    public void testAuthGetWithClientFilter() {
        client().register(new HttpBasicAuthFilter("name", "password"));
        Response response = target("test/filter").request().get();
        assertEquals("GET", response.readEntity(String.class));
    }

    @Test
    public void testAuthPostWithClientFilter() {
        client().register(new HttpBasicAuthFilter("name", "password"));
        Response response = target("test/filter").request().post(Entity.text("POST"));
        assertEquals("POST", response.readEntity(String.class));
    }


    @Test
    public void testAuthDeleteWithClientFilter() {
        client().register(new HttpBasicAuthFilter("name", "password"));
        Response response = target("test/filter").request().delete();
        assertEquals(204, response.getStatus());
    }

}
