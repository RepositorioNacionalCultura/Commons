package mx.gob.cultura.commons;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Inteface defining methods to implement by URLBuilders
 * @author Hasdai Pacheco
 */
public interface URLBuilder {
    /**
     * Sets base URL
     * @param baseURL base URL
     * @return URLBuilder object for chaining method invocations
     */
    URLBuilder setBaseURL(String baseURL);

    /**
     * Adds a query parameter to the URLBuilder
     * @param param parameter name
     * @param value parameter value
     * @return URLBuilder object for chaining method invocations
     */
    URLBuilder putQueryParameter(String param, String value);

    /**
     * Removes a query parameter from URLBuilder
     * @param param parameter name
     * @return URLBuilder object for chaining method invocations
     */
    URLBuilder removeQueryParameter(String param);

    /**
     * Remove all query parameters from URLBuilder
     * @return URLBuilder object for chaining method invocations
     */
    URLBuilder removeQueryParameters();

    /**
     * Gets the parameter map of URLBuilder
     * @return parameter map
     */
    Map<String, String> getQueryParameterMap();

    /**
     * Build URL object from URLBuilder properties
     * @return {@link URL} built with {@link URLBuilder} configuration
     * @throws MalformedURLException if URL is not valid.
     */
    URL build() throws MalformedURLException;
}
