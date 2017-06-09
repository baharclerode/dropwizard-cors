package zone.dragon.dropwizard.cors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that non-simple headers returned by this resource method or any child resources are to be exposed to cross-origin clients.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CorsExposeHeaders {
    /**
     * List of response headers to expose to cross-origin clients; If empty, all non-simple headers will be exposed.
     */
    String[] value() default {};
}
