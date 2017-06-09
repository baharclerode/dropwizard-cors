package zone.dragon.dropwizard.cors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that non-simple headers will be accepted by this resource from cross-origin clients
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CorsAllowHeaders {
    /**
     * List of response headers to accept from cross-origin clients; If empty, all non-simple headers will be accepted.
     */
    String[] value() default {};
}
