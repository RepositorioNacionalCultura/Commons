package mx.gob.cultura.commons;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Base implementation of URLBuilder Interface.
 * @author Hasdai Pacheco
 */
public class URLBuilderBase implements URLBuilder {
    private String baseURL;
    private final TreeMap<String, String> params;

    /**
     * Constructor. Creates a new instance of {@link URLBuilderBase}
     * @param baseURL base URL
     */
    public URLBuilderBase(String baseURL) {
        this.baseURL = baseURL;
        params = new TreeMap<>();
    }

    @Override
    public URLBuilder setBaseURL(String baseURL) {
        this.baseURL = baseURL;
        return this;
    }

    @Override
    public URLBuilder putQueryParameter(String param, String value) {
        params.put(param, value);
        return this;
    }

    @Override
    public URLBuilder removeQueryParameter(String param) {
        params.remove(param);
        return this;
    }

    @Override
    public URLBuilder removeQueryParameters() {
        params.clear();
        return this;
    }

    @Override
    public Map<String, String> getQueryParameterMap() {
        return params;
    }

    @Override
    public URL build() throws MalformedURLException {
        StringBuilder theUrl = new StringBuilder();
        theUrl.append(baseURL);
        Iterator<String> parIt = params.keySet().iterator();

        if (parIt.hasNext()) {
            theUrl.append("?");
            while (parIt.hasNext()) {
                String p = parIt.next();
                theUrl.append(p).append("=").append(params.get(p));
                if (parIt.hasNext())
                    theUrl.append("&");
            }
        }

        return new URL(theUrl.toString());
    }
}
