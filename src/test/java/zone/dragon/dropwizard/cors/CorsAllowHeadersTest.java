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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static zone.dragon.dropwizard.cors.CorsResponseFilter.ALLOW_HEADERS;
import static zone.dragon.dropwizard.cors.CorsResponseFilter.REQUEST_HEADERS;
import static zone.dragon.dropwizard.cors.CorsResponseFilter.REQUEST_METHOD;

public class CorsAllowHeadersTest {
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
        @DELETE
        public String testDefaultExposeHeaders() {
            return "Delete";
        }

        @PUT
        @CorsAllowHeaders({"Test", "Test2"})
        public Response testExplicitExposeHeaders() {
            return Response.ok("put").header("Test", "value1").header("Test2", "value2").build();
        }

        @GET
        @CorsAllowHeaders
        public Response testImplicitExposeHeaders() {
            return Response.ok("hello").header("Header-1", "value1").build();
        }
    }

    protected WebTarget client = APP_RULE.client().target(String.format("http://localhost:%d", APP_RULE.getLocalPort()));

    @Test
    public void testDefaultAllowHeaders() {
        MultivaluedMap<String, String> stringHeaders = client
            .path("someUri")
            .request()
            .header(REQUEST_HEADERS, "Test")
            .header(REQUEST_METHOD, "DELETE")
            .options()
            .getStringHeaders();
        assertThat(stringHeaders.getFirst(ALLOW_HEADERS)).isEqualTo(null);
    }

    @Test
    public void testExplicitAllowHeaders() {
        MultivaluedMap<String, String> stringHeaders = client
            .path("someUri")
            .request()
            .header(REQUEST_METHOD, "PUT")
            .header(REQUEST_HEADERS, "Test, Test2, Test3")
            .options()
            .getStringHeaders();
        assertThat(stringHeaders.getFirst(ALLOW_HEADERS).split("\\s*,\\s*")).containsExactlyInAnyOrder("Test", "Test2");
    }

    @Test
    public void testImplicitAllowHeaders() {
        MultivaluedMap<String, String> stringHeaders = client
            .path("someUri")
            .request()
            .header(REQUEST_METHOD, "GET")
            .header(REQUEST_HEADERS, "Test, Test2, Test3")
            .options()
            .getStringHeaders();
        assertThat(stringHeaders.getFirst(ALLOW_HEADERS)).isEqualTo("Test, Test2, Test3");
    }
}
