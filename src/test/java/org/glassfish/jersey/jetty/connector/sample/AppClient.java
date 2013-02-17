package org.glassfish.jersey.jetty.connector.sample;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.jetty.connector.JettyConnector;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


/**
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
public class AppClient {
    public static void main(String[] args) throws Exception {
        ClientConfig cc = new ClientConfig();
        cc.register(new LoggingFilter());
        cc.connector(new JettyConnector(cc));

        Client client = ClientBuilder.newClient(cc);
        WebTarget target = client.target(AppMain.BASE_URI);

        Response response = target.path(AppMain.PATH).request().get();
        String entity = response.readEntity(String.class);
        System.out.println(entity);

        Future<Response> future = target.path(AppMain.PATH).request().async().get();
        response = future.get(3, TimeUnit.SECONDS);
        entity = response.readEntity(String.class);
        System.out.println(entity);

        client.close();
    }
}
