package zone.dragon.dropwizard.cors;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.ResourceMethod;
import zone.dragon.dropwizard.cors.annotations.CorsAllowCredentials;
import zone.dragon.dropwizard.cors.annotations.CorsAllowHeaders;
import zone.dragon.dropwizard.cors.annotations.CorsAllowOrigins;
import zone.dragon.dropwizard.cors.annotations.CorsExposeHeaders;
import zone.dragon.dropwizard.cors.annotations.CorsMaxAge;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response.Status.Family;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@javax.ws.rs.ext.Provider
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Singleton
public class CorsResponseFilter implements ContainerResponseFilter {
    public static final String               ALLOW_METHODS     = "Access-Control-Allow-Methods";
    public static final String               ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String               ALLOW_HEADERS     = "Access-Control-Allow-Headers";
    public static final String               ALLOW_ORIGIN      = "Access-Control-Allow-Origin";
    public static final String               EXPOSE_HEADERS    = "Access-Control-Expose-Headers";
    public static final String               MAX_AGE           = "Access-Control-Max-Age";
    public static final String               REQUEST_METHOD    = "Access-Control-Request-Method";
    public static final String               REQUEST_HEADERS   = "Access-Control-Request-Headers";
    public static final String               WILDCARD_ORIGIN   = "*";
    public static final ImmutableSet<String> SIMPLE_HEADERS    = ImmutableSet.of(
        HttpHeaders.CACHE_CONTROL,
        HttpHeaders.CONTENT_LANGUAGE,
        HttpHeaders.CONTENT_TYPE,
        HttpHeaders.EXPIRES,
        HttpHeaders.LAST_MODIFIED,
        HttpHeaders.PRAGMA
    );
    @NonNull
    private final Provider<ExtendedUriInfo> uriInfo;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        ExtendedUriInfo uriInfo = this.uriInfo.get();
        // Standard Requests
        CorsAllowOrigins allowOrigins = findResourceAnnotation(uriInfo, CorsAllowOrigins.class);
        if (allowOrigins != null) {
            for (String allowedOrigin : allowOrigins.value()) {
                if (allowedOrigin.equals(WILDCARD_ORIGIN) || allowedOrigin.equals(requestContext.getHeaderString(HttpHeaders.ORIGIN))) {
                    CorsExposeHeaders exposeHeaders = findResourceAnnotation(uriInfo, CorsExposeHeaders.class);
                    if (exposeHeaders != null) {
                        String[] exposedHeaders = exposeHeaders.value();
                        if (exposedHeaders.length == 0) {
                            exposedHeaders = responseContext
                                .getHeaders()
                                .keySet()
                                .stream()
                                .filter(header -> !SIMPLE_HEADERS.contains(header))
                                .toArray(String[]::new);
                        }
                        responseContext.getHeaders().add(EXPOSE_HEADERS, Joiner.on(", ").join(exposedHeaders));
                    }
                    responseContext.getHeaders().add(ALLOW_ORIGIN, allowedOrigin);
                    CorsAllowCredentials allowCredentials = findResourceAnnotation(uriInfo, CorsAllowCredentials.class);
                    if (allowCredentials != null) {
                        responseContext.getHeaders().add(ALLOW_CREDENTIALS, "true");
                    }
                    break;
                }
            }
            // TODO add Origin header to Vary
        }
        // Pre-Flight Requests
        if (!HttpMethod.OPTIONS.equals(requestContext.getMethod()) || responseContext.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
            return;
        }
        String               corsMethod              = requestContext.getHeaderString(REQUEST_METHOD);
        List<ResourceMethod> locators                = uriInfo.getMatchedResourceLocators();
        List<ResourceMethod> resourceMethods         = uriInfo.getMatchedModelResource().getResourceMethods();
        Set<String>          corsMethods             = Sets.newHashSet();
        ResourceMethod       requestedResourceMethod = null;
        // Find resources that support CORS and build the Access-Control-Allow-Methods header
        for (ResourceMethod resourceMethod : resourceMethods) {
            allowOrigins = findResourceAnnotation(locators, resourceMethod, CorsAllowOrigins.class);
            if (allowOrigins != null) {
                corsMethods.add(resourceMethod.getHttpMethod());
                if (resourceMethod.getHttpMethod().equals(corsMethod)) {
                    requestedResourceMethod = resourceMethod;
                }
            }
        }
        if (!corsMethods.isEmpty()) {
            responseContext.getHeaders().add(ALLOW_METHODS, Joiner.on(", ").join(corsMethods));
        }
        if (requestedResourceMethod == null) {
            return;
        }
        // Scan for additional annotations that affect pre-flight requests
        CorsMaxAge maxAge = findResourceAnnotation(locators, requestedResourceMethod, CorsMaxAge.class);
        if (maxAge != null) {
            responseContext.getHeaders().add(MAX_AGE, maxAge.value());
        }
        CorsAllowCredentials allowCredentials = findResourceAnnotation(locators, requestedResourceMethod, CorsAllowCredentials.class);
        if (allowCredentials != null) {
            responseContext.getHeaders().add(ALLOW_CREDENTIALS, "true");
        }
        String requestedHeaders = requestContext.getHeaderString(REQUEST_HEADERS);
        if (requestedHeaders != null && !requestedHeaders.isEmpty()) {
            CorsAllowHeaders allowHeaders = findResourceAnnotation(locators, requestedResourceMethod, CorsAllowHeaders.class);
            if (allowHeaders != null) {
                List<String> allowedHeaders = Lists.newArrayList(requestedHeaders.split("\\s*,\\s*"));
                if (allowHeaders.value().length != 0) {
                    allowedHeaders.retainAll(Arrays.asList(allowHeaders.value()));
                }
                if (!allowedHeaders.isEmpty()) {
                    responseContext.getHeaders().add(ALLOW_HEADERS, Joiner.on(", ").join(allowedHeaders));
                }
            }
        }
    }

    protected <T extends Annotation> T findResourceAnnotation(ExtendedUriInfo uriInfo, Class<T> annotationType) {
        return findResourceAnnotation(uriInfo.getMatchedResourceLocators(), uriInfo.getMatchedResourceMethod(), annotationType);
    }

    protected <T extends Annotation> T findResourceAnnotation(
        List<ResourceMethod> locators, ResourceMethod method, Class<T> annotationType
    ) {
        List<ResourceMethod> resourceMethods = Lists.newArrayList(locators);
        resourceMethods.add(0, method);
        for (int i = 0; i < resourceMethods.size(); i++) {
            T annotation = findResourceAnnotation(resourceMethods.get(i), annotationType);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Attempts to find an annotation on a jersey resource method by first looking at the invocable's handler method, and if not found
     * there,  then at the handling class itself.
     *
     * @param method
     *     Resource method to search for the {@code annotationType}
     * @param annotationType
     *     Type of annotation
     *
     * @return Instance of the annotation if it was found, or {@code null} if the annotation does not exist on the resource
     */
    protected <T extends Annotation> T findResourceAnnotation(ResourceMethod method, Class<T> annotationType) {
        T annotation = method.getInvocable().getHandlingMethod().getAnnotation(annotationType);
        if (annotation == null) {
            return method.getInvocable().getHandler().getHandlerClass().getAnnotation(annotationType);
        }
        return annotation;
    }
}
