package zone.dragon.dropwizard.cors;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import zone.dragon.dropwizard.cors.annotations.CorsAllowCredentials;
import zone.dragon.dropwizard.cors.annotations.CorsAllowOrigins;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;

import static org.assertj.core.api.Assertions.assertThat;
import static zone.dragon.dropwizard.cors.CorsResponseFilter.ALLOW_CREDENTIALS;
import static zone.dragon.dropwizard.cors.CorsResponseFilter.REQUEST_METHOD;

public class CorsAllowCredentialsTest {
    @ClassRule
    public static final DropwizardAppRule<Configuration> APP_RULE = new DropwizardAppRule<>(TestApp.class, new Configuration());

    static {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
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
    @CorsAllowOrigins
    public static class TestResource {
        @GET
        @CorsAllowCredentials
        public String testDisabledMaxAge() {
            return "get";
        }

        @DELETE
        public String testNonexistentMaxAge() {
            return "Delete";
        }
    }

    protected WebTarget client = APP_RULE.client().target(String.format("http://localhost:%d", APP_RULE.getLocalPort()));

    @Test
    public void testPreflightAllowCredentials() {
        MultivaluedMap<String, String> stringHeaders = client
            .path("someUri")
            .request()
            .header(REQUEST_METHOD, "GET")
            .options()
            .getStringHeaders();
        assertThat(stringHeaders.getFirst(ALLOW_CREDENTIALS)).isEqualTo("true");
    }

    @Test
    public void testPreflightNotAllowCredentials() {
        MultivaluedMap<String, String> stringHeaders = client
            .path("someUri")
            .request()
            .header(REQUEST_METHOD, "DELETE")
            .options()
            .getStringHeaders();
        assertThat(stringHeaders.getFirst(ALLOW_CREDENTIALS)).isEqualTo(null);
    }

    @Test
    public void testNonPreflightOptionsRequest() {
        MultivaluedMap<String, String> stringHeaders = client
            .path("someUri")
            .request()
            .options()
            .getStringHeaders();
        assertThat(stringHeaders.getFirst(ALLOW_CREDENTIALS)).isEqualTo(null);
    }

    @Test
    public void testRegularRequestAllowCredentials() {
        MultivaluedMap<String, String> stringHeaders = client.path("someUri").request().get().getStringHeaders();
        assertThat(stringHeaders.getFirst(ALLOW_CREDENTIALS)).isEqualTo("true");
    }

    @Test
    public void testRegularRequestNoAllowCredentials() {
        MultivaluedMap<String, String> stringHeaders = client.path("someUri").request().delete().getStringHeaders();
        assertThat(stringHeaders.getFirst(ALLOW_CREDENTIALS)).isEqualTo(null);
    }

}
