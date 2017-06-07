package zone.dragon.dropwizard.cors;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import zone.dragon.dropwizard.cors.annotations.CorsAllowHeaders;
import zone.dragon.dropwizard.cors.annotations.CorsAllowOrigins;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static zone.dragon.dropwizard.cors.CorsResponseFilter.REQUEST_HEADERS;
import static zone.dragon.dropwizard.cors.CorsResponseFilter.REQUEST_METHOD;

/**
 * @author Darth Android
 * @date 6/3/2017
 */
public class CorsBundleTest {

    static {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    @ClassRule
    public static final DropwizardAppRule<Configuration> APP_RULE = new DropwizardAppRule<>(TestApp.class, new Configuration());

    public static class NestedTestResource {
        @GET
        public String testNestedResource() {
            return "nestedResource";
        }

        @POST
        public String testPostedNestedResource() {
            return "postResource";
        }

        @CorsAllowOrigins
        @Path("nested")
        public Class<?> getNext() {
            return NestedNestedTestResource.class;
        }
    }

    public static class NestedNestedTestResource {
        @GET
        @CorsAllowHeaders("X-My-Test-Header")
        public String testNestedResource() {
            return "nestedResource";
        }

        @POST
        public String testPostedNestedResource() {

            return "postResource";
        }
    }

    public static class TestApp extends Application<Configuration> {
        @Override
        public void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(new CorsBundle());
        }

        @Override
        public void run(Configuration configuration, Environment environment) throws Exception {
            environment.jersey().register(TestResource.class);
        }
    }

    @Path("someUri")
    public static class TestResource {
        @DELETE
        public String testDeleteResource() {
            return "delete";
        }

        @Path("nestedUri")
        public Class<NestedTestResource> testReource() {
            return NestedTestResource.class;
        }
    }

    @Test
    public void testQuotedProperty() {
        System.out.println(APP_RULE
                               .client()
                               .target(String.format("http://localhost:%d", APP_RULE.getLocalPort()))
                               .path("someUri")
                               .path("nestedUri")
                               .path("nested")
                               .request()
                               .header(REQUEST_HEADERS, "X-My-Test-Header")
                               .header(REQUEST_METHOD, "POST")
                               .options().getStringHeaders());
    }
}
