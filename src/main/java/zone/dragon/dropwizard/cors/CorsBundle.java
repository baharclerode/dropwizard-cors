package zone.dragon.dropwizard.cors;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class CorsBundle implements Bundle {
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // not used
    }

    @Override
    public void run(Environment environment) {
        environment.jersey().register(CorsResponseFilter.class);
    }
}
