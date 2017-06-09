package zone.dragon.dropwizard.cors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables CORS for a resource by indicating what origins are allowed to call this resource
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CorsAllowOrigins {
    /**
     * List of cross-origins that are allowed to call this resource, or {@code "*"} if any origin can call this resource (the default)
     */
    String[] value() default {"*"};
}
