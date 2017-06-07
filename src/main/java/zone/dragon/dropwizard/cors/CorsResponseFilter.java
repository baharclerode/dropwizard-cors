package zone.dragon.dropwizard.cors;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.Resource;
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
    public static final String ALLOW_METHODS     = "Access-Control-Allow-Methods";
    public static final String ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String ALLOW_HEADERS     = "Access-Control-Allow-Headers";
    public static final String ALLOW_ORIGIN      = "Access-Control-Allow-Origin";
    public static final String EXPOSE_HEADERS    = "Access-Control-Expose-Headers";
    public static final String MAX_AGE           = "Access-Control-Max-Age";
    public static final String REQUEST_METHOD    = "Access-Control-Request-Method";
    public static final String REQUEST_HEADERS   = "Access-Control-Request-Headers";
    public static final String WILDCARD_ORIGIN   = "*";
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
                    responseContext.getHeaders().add(ALLOW_ORIGIN, allowedOrigin);
                    CorsAllowCredentials allowCredentials = findResourceAnnotation(uriInfo, CorsAllowCredentials.class);
                    if (allowCredentials != null) {
                        responseContext.getHeaders().add(ALLOW_CREDENTIALS, "true");
                    }
                    CorsExposeHeaders exposeHeaders = findResourceAnnotation(uriInfo, CorsExposeHeaders.class);
                    if (exposeHeaders != null) {
                        responseContext.getHeaders().add(EXPOSE_HEADERS, Joiner.on(", ").join(exposeHeaders.value()));
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
        String               corsMethod      = requestContext.getHeaderString(REQUEST_METHOD);
        List<ResourceMethod> locators        = uriInfo.getMatchedResourceLocators();
        Resource             resource        = uriInfo.getMatchedModelResource();
        List<ResourceMethod> resourceMethods = resource.getResourceMethods();
        Set<String>          corsMethods     = Sets.newHashSet();
        for (ResourceMethod resourceMethod : resourceMethods) {
            allowOrigins = findResourceAnnotation(locators, resourceMethod, CorsAllowOrigins.class);
            if (allowOrigins != null) {
                corsMethods.add(resourceMethod.getHttpMethod());
                if (resourceMethod.getHttpMethod().equals(corsMethod)) {
                    CorsMaxAge maxAge = findResourceAnnotation(locators, resourceMethod, CorsMaxAge.class);
                    if (maxAge != null) {
                        responseContext.getHeaders().add(MAX_AGE, maxAge.value());
                    }
                    String requestedHeaders = requestContext.getHeaderString(REQUEST_HEADERS);
                    if (requestedHeaders != null && !requestedHeaders.isEmpty()) {
                        CorsAllowHeaders allowHeaders = findResourceAnnotation(locators, resourceMethod, CorsAllowHeaders.class);
                        if (allowHeaders != null) {
                            List<String> allowedHeaders = Arrays.asList(allowHeaders.value());
                            allowedHeaders.retainAll(Arrays.asList(requestedHeaders.split("\\s*,\\s*")));
                            if (!allowedHeaders.isEmpty()) {
                                responseContext.getHeaders().add(ALLOW_HEADERS, Joiner.on(", ").join(allowedHeaders));
                            }
                        }
                    }
                }
            }
        }
        if (!corsMethods.isEmpty()) {
            responseContext.getHeaders().add(ALLOW_METHODS, Joiner.on(", ").join(corsMethods));
        }
    }

    protected <T extends Annotation> T findResourceAnnotation(ExtendedUriInfo uriInfo, Class<T> annotationType) {
        return findResourceAnnotation(uriInfo.getMatchedResourceLocators(), uriInfo.getMatchedResourceMethod(), annotationType);
    }

    protected <T extends Annotation> T findResourceAnnotation(
        List<ResourceMethod> locators, ResourceMethod resourceMethod, Class<T> annotationType
    ) {
        List<ResourceMethod> resourceMethods = Lists.newArrayList(locators);
        resourceMethods.add(0, resourceMethod);
        for (int i = 0; i < resourceMethods.size(); i++) {
            T annotation = findResourceAnnotation(resourceMethods.get(i), annotationType);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    protected <T extends Annotation> T findResourceAnnotation(ResourceMethod method, Class<T> annotationType) {
        T annotation = method.getInvocable().getHandlingMethod().getAnnotation(annotationType);
        if (annotation == null) {
            return method.getInvocable().getHandler().getHandlerClass().getAnnotation(annotationType);
        }
        return annotation;
    }

    protected boolean isCorsEnabled(ResourceMethod method) {
        System.out.println("Method: " + method.getHttpMethod());
        System.out.println("Parent: " + method.getParent());
        System.out.println("Handler Class: " + method.getInvocable().getHandler().getHandlerClass().getName());
        System.out.println("Handler Method: " + method.getInvocable().getHandlingMethod().getName());
        return true;
    }

    protected boolean isWildcardOriginAllowed(CorsAllowOrigins allowedOrigins) {
        for (String origin : allowedOrigins.value()) {
            if (WILDCARD_ORIGIN.equals(origin)) {
                return true;
            }
        }
        return false;
    }
}
