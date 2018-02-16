package mx.gob.cultura.commons.servlet;

import mx.gob.cultura.commons.Util;
import mx.gob.cultura.commons.config.AppConfig;
import org.apache.log4j.Logger;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * {@link ServletContextListener} that manages ElasticSearch client creation and index initialization.
 * @author Hasdai Pacheco
 */
public class ElasticServletContextListener implements ServletContextListener {
    private static final Logger logger = Logger.getLogger(ElasticServletContextListener.class);
    private RestHighLevelClient c;
    final AppConfig config;

    /**
     * Constructor. Creates a new instance of {@link ElasticServletContextListener}.
     */
    public ElasticServletContextListener () {
        config = loadConfigProperties();
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        c = Util.ELASTICSEARCH.getElasticClient(config.getElasticHost(), config.getElasticPort());

        //Remove test index if env is production
        if (Util.ENV_PRODUCTION.equals(config.getEnvName())) {
            try {
                logger.trace("Removing test index...");
                c.getLowLevelClient().performRequest("DELETE", Util.ELASTICSEARCH.REPO_INDEX_TEST);
            } catch (IOException ioex) {
                logger.info("Test index was not removed");
            }
        } else {
            try {
                Response resp = c.getLowLevelClient().performRequest("HEAD", Util.ELASTICSEARCH.REPO_INDEX_TEST);
                if(resp.getStatusLine().getStatusCode() != RestStatus.OK.getStatus()) {
                    if (createESTestIndex()) {
                        logger.trace("Created test index");
                    } else {
                        logger.trace("Test index could not be created");
                    }
                } else {
                    logger.info("Index "+ Util.ELASTICSEARCH.REPO_INDEX_TEST +" already exists...");
                }
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        //Close clients
        Util.ELASTICSEARCH.closeElasticClients();
    }

    /**
     * Loads configuration from application properties.
     * @return {@link AppConfig} object.
     */
    private AppConfig loadConfigProperties() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("app.properties");
        Properties props = new Properties();

        if (null != is) {
            try {
                props.load(is);
            } catch (IOException ioex) {
                logger.error("Error loading properties file", ioex);
            }
        }

        return AppConfig.getConfigObject(props);
    }

    /**
     * Creates default ElasticSearch test index for cultural objects.
     * @return true if creation succeeds, false otherwise
     */
    private boolean createESTestIndex() {
        boolean ret = false;
        logger.trace("Creating index "+ Util.ELASTICSEARCH.REPO_INDEX_TEST +"...");
        InputStream is = getClass().getClassLoader().getResourceAsStream("indexmapping_cultura.json");
        if (null != is) {
            String mapping = Util.FILE.readFromStream(is, StandardCharsets.UTF_8.name());
            JSONObject mp = new JSONObject(mapping);
            JSONObject aliases = new JSONObject();
            aliases.put(config.getIndexName(), new JSONObject());
            mp.put("aliases", aliases);

            ret = Util.ELASTICSEARCH.createIndex(c, Util.ELASTICSEARCH.REPO_INDEX_TEST, mp.toString());

            if (ret) {
                logger.info("Index " + Util.ELASTICSEARCH.REPO_INDEX_TEST + " created with alias "+ config.getIndexName());
            }
        }

        //Load test data
        if (ret) {
            InputStream datas = getClass().getClassLoader().getResourceAsStream("data.json");
            if (null != datas) {
                ArrayList<String> objs = new ArrayList<>();
                String jsonString = Util.FILE.readFromStream(datas, StandardCharsets.UTF_8.name());

                try {
                    JSONArray data = new JSONArray(jsonString);
                    logger.trace("Loading "+ data.length() +" objects from test data");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject o = data.getJSONObject(i);
                        o.put("indexcreated", System.currentTimeMillis());
                        objs.add(o.toString());
                    }
                } catch (JSONException jsex) {
                    logger.error("Unable to load sample data", jsex);
                }

                List<String> indexed = Util.ELASTICSEARCH.indexObjects(c, Util.ELASTICSEARCH.REPO_INDEX_TEST, config.getIndexType(), objs);
            }
        }
        return ret;
    }
}
