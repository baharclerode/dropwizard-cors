package zone.dragon.dropwizard.cors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the maximum time a pre-flight request can be cached before another pre-flight request must be issued
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CorsMaxAge {
    /**
     * Flag value representing that pre-flight requests may not be cached at all, and should be issued for every request
     */
    long DISABLED = -1;

    /**
     * Number of seconds since the pre-flight response was sent that it remains valid
     */
    long value();
}
