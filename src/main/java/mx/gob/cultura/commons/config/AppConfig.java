package mx.gob.cultura.commons.config;

import mx.gob.cultura.commons.Util;

import java.util.Properties;

/**
 * Singleton for holding application configuration properties.
 * @author Hasdai Pacheco.
 */
public class AppConfig {
    //Environment config
    private String envName;

    //ES Configuration
    private final String elasticHost;
    private int elasticPort;
    private String indexName;
    private final String indexType;

    //MongoDB Configuration
    private final String mongoHost;
    private int mongoPort;
    private final String mongoUser;
    private final String mongoPass;
    
    private final String oaiPattern;

    private static AppConfig instance = null;

    /**
     * Constructor. Creates a new instance of {@link AppConfig}
     * @param props Configuration properties
     */
    private AppConfig(Properties props) {
        //Get environment configuration
        envName = props.getProperty("environment.name","").toUpperCase();
        if (envName.isEmpty()) {
            envName = Util.getEnvironmentName();
        }

        //Get ElasticSearch configuration
        elasticHost = props.getProperty("elastic.host", "localhost");
        try {
            elasticPort = Integer.parseInt(props.getProperty("elastic.port", "9200"));
        } catch (NumberFormatException nfe) {
            elasticPort = 9200;
        }

        indexType = props.getProperty("elastic.indexType", "bic");
        indexName = props.getProperty("elastic.indexName", "");
        if (indexName.isEmpty()) {
            indexName = Util.ENV_DEVELOPMENT.equals(envName) ? Util.ELASTICSEARCH.REPO_INDEX_TEST : Util.ELASTICSEARCH.REPO_INDEX;
        }

        //Get MongoDB Configuration
        mongoHost = props.getProperty("mongo.host", "localhost");
        try {
            mongoPort = Integer.parseInt(props.getProperty("mongo.port", "27017"));
        } catch (NumberFormatException nfe) {
            mongoPort = 27017;
        }
        mongoUser = props.getProperty("mongo.user", "");
        mongoPass = props.getProperty("mongo.pass", "");
        
        oaiPattern = props.getProperty("oai.pattern","oai:mexicana.cultura.gob.mx:{@idHolder}/");
        
    }

    /**
     * Gets the {@link AppConfig} object holding application configuration.
     * @param props {@link Properties} for object initialization.
     * @return Instance of {@link AppConfig} object with loaded properties.
     */
    public static AppConfig getConfigObject(Properties props) {
        if (null == instance) {
            instance = new AppConfig(props);
        }

        return instance;
    }

    /**
     * Gets the {@link AppConfig} object holding default configuration.
     * @return Instance of {@link AppConfig} object with default properties.
     */
    public static AppConfig getConfigObject() {
        if (null == instance) {
            instance = new AppConfig(new Properties());
        }

        return instance;
    }

    public String getElasticHost() {
        return elasticHost;
    }

    public int getElasticPort() {
        return elasticPort;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getIndexType() {
        return indexType;
    }

    public String getMongoHost() {
        return mongoHost;
    }

    public int getMongoPort() {
        return mongoPort;
    }
    
    public String getMongoUser() {
        return mongoUser;
    }
    
    public String getMongoPass() {
        return mongoPass;
    }
    
    public String getOAIPattern() {
        return oaiPattern;
    }

    public String getEnvName() {
        return envName;
    }
}
