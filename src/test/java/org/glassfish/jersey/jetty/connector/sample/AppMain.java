package org.glassfish.jersey.jetty.connector.sample;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.*;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public class AppMain {

    public static final String PATH = "test";
    public static final URI BASE_URI = URI.create("http://localhost:8080/");
    private static final Logger LOGGER = Logger.getLogger(AppMain.class.getName());


    @Path("/test")
    public static class HttpMethodResource {
        @GET
        public String get() {
            return "GET";
        }

        @POST
        public String post(String entity) {
            return entity;
        }

        @PUT
        public String put(String entity) {
            return entity;
        }

        @DELETE
        public String delete() {
            return "DELETE";
        }
    }


    public static void main(String[] args) {
        try {
            System.out.println("\"Example\" Jersey Application");
            final ResourceConfig resourceConfig = new ResourceConfig(HttpMethodResource.class);
            resourceConfig.registerInstances(new LoggingFilter(LOGGER, true));
            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, resourceConfig);
            System.out.println(String.format("Application started.\nTry out %s%s\nHit enter to stop it...", BASE_URI, PATH));
            System.in.read();
            server.stop();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
}