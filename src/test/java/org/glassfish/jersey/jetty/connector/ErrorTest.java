package org.glassfish.jersey.jetty.connector;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public class ErrorTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(ErrorTest.class.getName());

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(ErrorResource.class);
        config.register(new LoggingFilter(LOGGER, true));
        return config;
    }


    @Override
    protected void configureClient(ClientConfig clientConfig) {
        clientConfig.connector(new JettyConnector(clientConfig));
    }


    @Path("/test")
    public static class ErrorResource {
        @POST
        public Response post(String entity) {
            return Response.serverError().build();
        }

        @Path("entity")
        @POST
        public Response postWithEntity(String entity) {
            return Response.serverError().entity("error").build();
        }
    }

    @Test
    public void testPostError() {
        WebTarget r = target("test");

        for (int i = 0; i < 100; i++) {
            try {
                r.request().post(Entity.text("POST"));
            } catch (ClientErrorException ex) {
            }
        }
    }

    @Test
    public void testPostErrorWithEntity() {
        WebTarget r = target("test");

        for (int i = 0; i < 100; i++) {
            try {
                r.request().post(Entity.text("POST"));
            } catch (ClientErrorException ex) {
                String s = ex.getResponse().readEntity(String.class);
                assertEquals("error", s);
            }
        }
    }
}
