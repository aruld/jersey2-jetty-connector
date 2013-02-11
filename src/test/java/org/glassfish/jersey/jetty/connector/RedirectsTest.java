package org.glassfish.jersey.jetty.connector;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.Assert;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: arul
 * Date: 2/9/13
 * Time: 10:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class RedirectsTest {

    public static void main(String[] args) throws Exception {

        HttpClient client = new HttpClient();
        client.start();

        // Do not follow redirects by default
        client.setFollowRedirects(false);

        ContentResponse response = client.newRequest("http://expertstalk.tumblr.com")
                // Follow redirects for this request only
                .followRedirects(false)
                .timeout(5, TimeUnit.SECONDS)
                .send();

        System.out.println(response.getStatus());

//        Assert.assertEquals(200, response.getStatus());
        client.stop();


    }
}
