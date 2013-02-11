package org.glassfish.jersey.jetty.connector;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainer;

import org.junit.Ignore;
import org.junit.Test;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * @author Martin Matula (martin.matula at oracle.com)
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public class FollowRedirectsTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(FollowRedirectsTest.class.getName());

    @Path("/test")
    public static class RedirectResource {
        @GET
        public String get() {
            return "GET";
        }

        @GET
        @Path("redirect")
        public Response redirect() {
            return Response.seeOther(UriBuilder.fromResource(RedirectResource.class).build()).build();
        }
    }

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(RedirectResource.class);
        config.register(new LoggingFilter(LOGGER, true));
        return config;
    }

    @Override
    protected Client getClient(TestContainer tc, ApplicationHandler applicationHandler) {
        Client c = super.getClient(tc, applicationHandler);
        ClientConfig cc = new ClientConfig().connector(new JettyConnector(c.getConfiguration()));
        return ClientFactory.newClient(cc);
    }

    @Test
    @Ignore("Throws NPE, need to investigate.")
    public void testDoFollow() {
        Response r = target("test/redirect").request().get();
        assertEquals(200, r.getStatus());
        assertEquals("GET", r.readEntity(String.class));
    }

    @Test
    public void testDontFollow() {
        WebTarget t = target("test/redirect");
        t.setProperty(ClientProperties.FOLLOW_REDIRECTS, false);
        assertEquals(303, t.request().get().getStatus());
    }
}