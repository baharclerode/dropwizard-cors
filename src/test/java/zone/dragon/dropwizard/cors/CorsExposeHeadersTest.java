package zone.dragon.dropwizard.cors;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import zone.dragon.dropwizard.cors.annotations.CorsAllowOrigins;
import zone.dragon.dropwizard.cors.annotations.CorsExposeHeaders;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static zone.dragon.dropwizard.cors.CorsResponseFilter.EXPOSE_HEADERS;

public class CorsExposeHeadersTest {
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
        @CorsExposeHeaders({"Test", "Test2"})
        public Response testExplicitExposeHeaders() {
            return Response.ok("put").header("Test", "value1").header("Test2", "value2").build();
        }

        @GET
        @CorsExposeHeaders
        public Response testImplicitExposeHeaders() {
            return Response.ok("hello").header("Header-1", "value1").build();
        }
    }

    protected WebTarget client = APP_RULE.client().target(String.format("http://localhost:%d", APP_RULE.getLocalPort()));

    @Test
    public void testDefaultExposeHeaders() {
        MultivaluedMap<String, String> stringHeaders = client.path("someUri").request().delete().getStringHeaders();
        assertThat(stringHeaders.getFirst(EXPOSE_HEADERS)).isEqualTo(null);
    }

    @Test
    public void testExplicitExposeHeaders() {
        MultivaluedMap<String, String> stringHeaders = client.path("someUri").request().put(Entity.entity("\"test\"", MediaType.WILDCARD_TYPE))
                                                             .getStringHeaders();
        assertThat(stringHeaders.getFirst(EXPOSE_HEADERS).split("\\s*,\\s*")).containsExactlyInAnyOrder("Test", "Test2");
    }

    @Test
    public void testImplicitExposeHeaders() {
        MultivaluedMap<String, String> stringHeaders = client.path("someUri").request().get().getStringHeaders();
        assertThat(stringHeaders.getFirst(EXPOSE_HEADERS)).isEqualTo("Header-1");
    }
}
