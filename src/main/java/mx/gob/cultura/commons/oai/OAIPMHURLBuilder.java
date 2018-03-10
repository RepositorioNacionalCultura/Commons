package mx.gob.cultura.commons.oai;

import mx.gob.cultura.commons.URLBuilder;
import mx.gob.cultura.commons.URLBuilderBase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Utility class to build OAI-PMH request URLs.
 * @author Hasdai Pacheco
 */
public class OAIPMHURLBuilder {
    private String baseURL;
    private final URLBuilder builder;

    /**
     * OAI-PMH accepted verbs.
     */
    public enum VERB {
        IDENTIFY, LISTIDENTIFIERS, LISTRECORDS, LISTSETS, LISTMETADATAFORMATS, GETRECORD
    }

    /**
     * Commonly used prefixes.
     */
    public enum PREFIX {
        MODS, MEDS, OAI_DC, OAI_ETDMS, ORE
    }

    /**
     * Constructor. Creates a new {@link OAIPMHURLBuilder}.
     * @param baseUrl Base URL
     */
    public OAIPMHURLBuilder(String baseUrl) {
        this.builder = new URLBuilderBase(baseUrl);
        this.setBaseURL(baseUrl);
    }

    /**
     * Sets the verb used in request URL
     * @param verb OAI-PMH Verb
     * @return {@link OAIPMHURLBuilder} object for chained method invocation
     */
    public OAIPMHURLBuilder setVerb(VERB verb) {
        builder.putQueryParameter("verb", null == verb ? null : verb.name());
        return this;
    }

    /**
     * Sets the prefix used in request URL
     * @param prefix OAI-PMH metadata prefix
     * @return {@link OAIPMHURLBuilder} object for chained method invocation
     */
    public OAIPMHURLBuilder setPrefix(PREFIX prefix) {
        builder.putQueryParameter("metadataPrefix", null == prefix ? null : prefix.name());
        return this;
    }

    /**
     * Sets the base URL for the request
     * @param baseURL OAI-PMH base URL
     * @return {@link OAIPMHURLBuilder} object for chained method invocation
     */
    public OAIPMHURLBuilder setBaseURL(String baseURL) {
        this.baseURL = baseURL;
        return this;
    }

    /**
     * Sets the token used in request URL
     * @param token OAI-PMH request token
     * @return {@link OAIPMHURLBuilder} object for chained method invocation
     */
    public OAIPMHURLBuilder setToken(String token) {
        builder.putQueryParameter("resumptionToken", token);
        return this;
    }

    /**
     * Sets the identifier used in request URL
     * @param identifier OAI-PMH object identifier
     * @return {@link OAIPMHURLBuilder} object for chained method invocation
     */
    public OAIPMHURLBuilder setIdentifier(String identifier) {
        builder.putQueryParameter("identifier", identifier);
        return this;
    }

    /**
     * Builds an URL object from saved configuration.
     * @return {@link URL} object with OAI-PMH request parameters set.
     * @throws MalformedURLException if provided parameters are not correct and URL can't be built.
     */
    public URL build() throws MalformedURLException {
        Map<String, String> params = builder.getQueryParameterMap();
        String verb = params.get("verb");
        String identifier = params.get("identifier");
        String token = params.get("resumptionToken");
        String prefix = params.get("metadataPrefix");

        boolean hasToken = null != token && !token.isEmpty();
        boolean hasMetadataPrefix = null != prefix && !prefix.isEmpty();
        boolean hasIdentifier = null != identifier && !identifier.isEmpty();

        if (null == baseURL || baseURL.isEmpty())
            throw new MalformedURLException("No base URL provided");

        if (null == verb || verb.isEmpty())
            throw new BadVerbException("Illegal verb");

        if (!hasToken && !hasMetadataPrefix && !verb.equals(VERB.IDENTIFY.name()))
            throw new BadArgumentException(verb+" must receive the metadataPrefix");

        if (hasToken && hasMetadataPrefix)
            throw new BadArgumentException("ResumptionToken cannot be sent together with from, until, metadataPrefix or set parameters");

        if (verb.equals(VERB.GETRECORD.name()) && !hasIdentifier)
            throw new BadArgumentException(verb+" verb requires the use of the parameters - identifier and metadataPrefix");

        if (verb.equals(VERB.GETRECORD.name()) && hasIdentifier && !hasMetadataPrefix)
            throw new BadArgumentException(verb+" must receive the metadataPrefix");

        if (verb.equals(VERB.GETRECORD.name()) && hasToken) {
            throw new BadArgumentException("ResumptionToken cannot be sent together with verb "+verb);
        }

        return builder.build();
    }

    /**
     * Exception for bad verb usage in OAI-PMH URL
     */
    public class BadVerbException extends MalformedURLException {
        public BadVerbException() {
            super();
        }

        public BadVerbException(String msg) {
            super(msg);
        }
    }

    /**
     * Exception for bad argument usage in OAI-PMH URL
     */
    public class BadArgumentException extends MalformedURLException {
        public BadArgumentException() {
            super();
        }

        public BadArgumentException(String msg) {
            super(msg);
        }
    }
}
