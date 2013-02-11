package org.glassfish.jersey.jetty.connector;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

/**
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public class GZIPContentEncodingTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(EntityTest.class.getName());

    @Path("/")
    public static class Resource {
        @POST
        public byte[] post(byte[] content) {
            return content;
        }
    }

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(Resource.class);
        config.register(new LoggingFilter(LOGGER, true));
        return config;
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        clientConfig.register(GZipEncoder.class);
        clientConfig.connector(new JettyConnector(clientConfig));
    }

    @Test
    public void testPost() {
        WebTarget r = target();
        byte[] content = new byte[1024 * 1024];
        assertTrue(Arrays.equals(content, r.request().post(Entity.entity(content, MediaType.APPLICATION_OCTET_STREAM_TYPE)).readEntity(byte[].class)));

        Response cr = r.request().post(Entity.entity(content, MediaType.APPLICATION_OCTET_STREAM_TYPE));
        assertTrue(cr.hasEntity());
        cr.close();
    }

    @Test
    public void testPostChunked() {
        ClientConfig cc = new ClientConfig();
        cc.setProperty(ClientProperties.CHUNKED_ENCODING_SIZE, 1024);
        cc.connector(new JettyConnector(cc));
        cc.register(new LoggingFilter(LOGGER, true));

        Client client = ClientFactory.newClient(cc);
        WebTarget r = client.target(getBaseUri());

        byte[] content = new byte[1024 * 1024];
        assertTrue(Arrays.equals(content, r.request().post(Entity.entity(content, MediaType.APPLICATION_OCTET_STREAM_TYPE)).readEntity(byte[].class)));

        Response cr = r.request().post(Entity.text("POST"));
        assertTrue(cr.hasEntity());
        cr.close();

        client.close();
    }

}