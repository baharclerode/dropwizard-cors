# Dropwizard Cross-Origin-Resource-Sharing

This bundle enables support for CORS headers, including pre-flight requests.

To use this bundle, add it to your application in the initialize method:

    @Override
    public void initialize(Bootstrap<YourConfig> bootstrap) {
        bootstrap.addBundle(new CorsBundle());
    }

### Configuring Headers

To enable basic CORS support for a resource, add the `@CorsAllowOrigins` annotation. By default, this whitelists all origins (equivalent 
to `@CorsAllowOrigins("*")`); To whitelist specific domains, provide the origins as an array with this annotation (Ex. `@CorsAllowOrigins
({"http://google.com", "https://google.com")`). This triggers generation of the `Access-Control-Allow-Origin` response header.

The following annotations can further control and customize the CORS response:

* `@CorsExposeHeaders` - Sets the custom headers that a browser is allowed to return with a cross-origin request with the 
`Access-Control-Expose-Headers` header in the response.
* `@CorsAllowHeaders` - Sets the custom headers that a browser is allowed to send with a cross-origin request with the 
`Access-Control-Allow-Headers` header in the pre-flight response.
* `@CorsAllowCredentials` - Sets if the browser is allowed to process credentials on a cross-origin request with the 
`Access-Control-Allow-Credentials` header in the response.
* `@CorsMaxAge(long)` - Sets the maximum duration that a preflight request is valid using the `Access-Control-Max-Age` header.

### Annotation Placement

In addition to placing annotations on resource methods, they can also be placed upon the class containing the resource methods (in which 
case they apply to all resource methods in that class), or they can be applied to parent resource methods/classes (in which case they 
apply to all child resources). If the same annotation appears in multiple places, the one closest to the resource method that was invoked
 will be used, and others will be ignored.