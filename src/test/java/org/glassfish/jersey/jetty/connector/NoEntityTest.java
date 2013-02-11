package org.glassfish.jersey.jetty.connector;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.logging.Logger;

/**
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public class NoEntityTest extends JerseyTest {
    private static final Logger LOGGER = Logger.getLogger(NoEntityTest.class.getName());

    @Path("/test")
    public static class HttpMethodResource {
        @GET
        public Response get() {
            return Response.status(Status.CONFLICT).build();
        }

        @POST
        public void post(String entity) {
        }
    }

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(HttpMethodResource.class);
        config.register(new LoggingFilter(LOGGER, true));
        return config;
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        clientConfig.connector(new JettyConnector(clientConfig));
    }

    @Test
    public void testGet() {
        WebTarget r = target("test");

        for (int i = 0; i < 5; i++) {
            Response cr = r.request().get();
        }
    }

    @Test
    public void testGetWithClose() {
        WebTarget r = target("test");
        for (int i = 0; i < 5; i++) {
            Response cr = r.request().get();
            cr.close();
        }
    }

    @Test
    public void testPost() {
        WebTarget r = target("test");
        for (int i = 0; i < 5; i++) {
            Response cr = r.request().post(null);
        }
    }

    @Test
    public void testPostWithClose() {
        WebTarget r = target("test");
        for (int i = 0; i < 5; i++) {
            Response cr = r.request().post(null);
            cr.close();
        }
    }
}