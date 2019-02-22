package mx.gob.cultura.commons;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.log4j.Logger;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.semanticwb.datamanager.DataList;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import mx.gob.cultura.commons.config.AppConfig;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONObject;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObjectIterator;

/**
 * Utility class with common methods.
 *
 * @author Hasdai Pacheco | Juan Antonio Fernandez
 */
public final class Util {

    private static final Logger logger = Logger.getLogger(Util.class);
    public static final String ENV_DEVELOPMENT = "DEV";
    public static final String ENV_TESTING = "TEST";
    public static final String ENV_QA = "QA";
    public static final String ENV_PRODUCTION = "PROD";
    private static HashMap<String, HashMap<String, String>> hmproplbl = null;

    private Util() {
    }

    public static String makeRequest(URL theUrl, boolean XMLSupport) {
        HttpURLConnection con = null;
        StringBuilder response = new StringBuilder();
        String errorMsg = null;
        int retries = 0;
        boolean isConnOk;

        do {
            logger.trace("Trying to make request to URL " + theUrl.toString());
            try {
                con = (HttpURLConnection) theUrl.openConnection();
                isConnOk = true;
                //AQUI SE PIDE EN XML
                if (XMLSupport) {
                    logger.trace("Setting XML request header");
                    con.setRequestProperty("accept", "application/xml");
                }
                con.setRequestMethod("GET");
                //System.out.println("content type:" + con.getContentType());

                int statusCode = con.getResponseCode();

                if (statusCode == 200) {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
                        String inputLine;

                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                    } catch (IOException ioex) {
                        ioex.printStackTrace();
                    }
                } else {
                    logger.debug("Server responded " + statusCode + " for " + theUrl);
                }
            } catch (IOException e) {
                logger.trace("Failed connection to URL " + theUrl + ". Retrying");
                if (null != con) {
                    con.disconnect();
                }
                retries++;
                try {
                    Thread.sleep(5000);
                } catch (Exception te) {
                    te.printStackTrace();
                }

                //e.printStackTrace();
                isConnOk = false;
                if (retries == 5) {
                    logger.trace("Max number of retries reached (" + retries + ")");
                    errorMsg = "#Error: No se puede conectar al servidor#";
                }
            }
        } while (!isConnOk && retries < 5);
        return errorMsg != null ? errorMsg : response.toString();
    }

    /**
     * Gets environment configuration.
     *
     * @return Value of REPO_DEVENV environment property. Defaults to
     * production.
     */
    public static String getEnvironmentName() {
        String env = System.getenv("REPO_DEVENV");
        if (null == env) {
            env = ENV_PRODUCTION;
        }
        return env;
    }

    /**
     * Inner class to encapsulate methods related to ElasticSearch actions.
     */
    public static final class ELASTICSEARCH {

        public static String REPO_INDEX = "cultura";
        public static String REPO_INDEX_TEST = "cultura_test";
        public static String REPO_TYPE = "bic";
        public static String ELASTIC_SERVER = AppConfig.getConfigObject().getElasticHost();
        ;
        public static int ELASTIC_PORT = AppConfig.getConfigObject().getElasticPort();
        ;

        private static final HashMap<String, RestHighLevelClient> elasticClients = new HashMap<>();

        public ELASTICSEARCH() {
            REPO_INDEX = AppConfig.getConfigObject().getIndexName();
            REPO_TYPE = AppConfig.getConfigObject().getIndexType();
            ELASTIC_SERVER = AppConfig.getConfigObject().getElasticHost();
            ELASTIC_PORT = AppConfig.getConfigObject().getElasticPort();
        }

        /**
         * Gets a {@link RestHighLevelClient} instance with default host and
         * port.
         *
         * @return RestHighLevelClient instance object.
         */
        public static RestHighLevelClient getElasticClient() {

            return getElasticClient(ELASTIC_SERVER, ELASTIC_PORT);
        }

        /**
         * Gets a {@link RestHighLevelClient} instance with given host and port.
         *
         * @param host ElasticSearch node host name.
         * @param port ElasticSearch node port.
         * @return RestHighLevelClient instance object.
         */
        public static RestHighLevelClient getElasticClient(String host, int port) {
            RestHighLevelClient ret = elasticClients.get(host + ":" + String.valueOf(port));
            if (null == ret) {
                ret = new RestHighLevelClient(
                        RestClient.builder(new HttpHost(host, port, "http")));

                elasticClients.put(host + ":" + String.valueOf(port), ret);
            }
            return ret;
        }

        /**
         * Closes an ElasticSearch {@link RestHighLevelClient} associated with
         *
         * @host and @port.
         *
         * @param host Hostname of client
         * @param port Port number of client
         */
        public static void closeElasticClient(String host, int port) {
            RestHighLevelClient ret = elasticClients.get(host + ":" + String.valueOf(port));
            if (null != ret) {
                try {
                    ret.close();
                    elasticClients.remove(host + ":" + String.valueOf(port), ret);
                } catch (IOException ioex) {
                    logger.error("Error while closing ES client", ioex);
                }
            }
        }

        /**
         * Closes all instances of {@link RestHighLevelClient}s.
         */
        public static void closeElasticClients() {
            for (RestHighLevelClient c : elasticClients.values()) {
                try {
                    c.close();
                } catch (IOException ioex) {
                    logger.error("Error while closing ES clients", ioex);
                }
            }
        }

        /**
         * Gets time-based UUID for indexing objects.
         *
         * @return String representation of a time-based UUID.
         */
        public static String getUUID() {
            return UUIDs.base64UUID();
        }

        /**
         * Indexes an object in ElasticSearch.
         *
         * @param client {@link RestHighLevelClient} object.
         * @param indexName Name of index to use.
         * @param typeName Name of type in index.
         * @param objectId ID for object, autogenerated if null.
         * @param objectJson Object content in JSON String format.
         * @return ID of indexed object or null if indexing fails.
         */
        public static String indexObject(RestHighLevelClient client, String indexName, String typeName, String objectId, String objectJson) {
            String ret = null;
            String id = objectId;

            if (null == objectId || objectId.isEmpty()) {
                id = Util.ELASTICSEARCH.getUUID();
            }

            IndexRequest req = new IndexRequest(indexName, typeName, id);
            req.source(objectJson, XContentType.JSON);

            try {
                IndexResponse resp = client.index(req);
                if (resp.status().getStatus() == RestStatus.CREATED.getStatus()
                        || resp.status().getStatus() == RestStatus.OK.getStatus()) {
                    ret = resp.getId();
                }
            } catch (IOException ioex) {
                logger.error("Error making index request for object with id " + objectId, ioex);
            }

            return ret;
        }

        /**
         * Remove Object from Index from the ElasticSearch.
         *
         * @param client {@link RestHighLevelClient} object.
         * @param indexName Name of index to use.
         * @param typeName Name of type in index.
         * @param objectId ID for object.
         * @return ID of indexed object or null if indexing fails.
         */
        public static boolean deleteIndexedObject(RestHighLevelClient client, String indexName, String typeName, String objectId) {
            boolean ret = false;
            String id = objectId;

//            if (null == objectId || objectId.isEmpty()) {
//                id = Util.ELASTICSEARCH.getUUID();
//            }
            DeleteRequest req = new DeleteRequest(indexName, typeName, id);
            //req.source(objectJson, XContentType.JSON);

            try {
                //DeleteResponse response = getElasticClient().prepareDelete(indexName, typeName, id).get();   
                DeleteResponse resp = client.delete(req);
                if (resp.status().getStatus() == RestStatus.OK.getStatus()) {
                    ret = true;
                }
            } catch (IOException ioex) {
                logger.error("Error removing document from index request for object with id " + objectId, ioex);
            }

            return ret;
        }

        /**
         * Search on Index by _id parameter value
         *
         * @param client ElasticSearch Client
         * @param indexName Name of index to use.
         * @param typeName Name of type in index.
         * @param objectId ID for object.
         * @return true if the index exists
         */
        public static boolean existsIndex(RestHighLevelClient client, String indexName, String typeName, String objectId) {
            boolean exists = false;
            //System.out.println("objectId: " + objectId);
            try {
                GetRequest getRequest = new GetRequest(indexName, typeName, objectId);
                //GetResponse getResponse = client.get(getRequest);
                exists = client.exists(getRequest);

            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
            return exists;
        }

        /**
         * Search For CulturaOAIID Property on Index by OAIID value
         *
         * @param client ElasticSearch Client
         * @param indexName Name of index to use.
         * @param typeName Name of type in index.
         * @param identifier oaiid value
         * @return culturaoaiid if the property exists, if not returns null.
         */
        public static String checkForCulturaOAIIDPropByIdentifier(RestHighLevelClient client, String indexName, String typeName, String identifier) {
            String ret = null;
            try {
                SearchRequest sreq = new SearchRequest(indexName);
                sreq.searchType(SearchType.DEFAULT);
                sreq.types(typeName);
//                System.out.println("Identifier:"+identifier);
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(QueryBuilders.termQuery("oaiid", identifier));
                sreq.source(searchSourceBuilder);
                SearchResponse resp = client.search(sreq);
                if (resp != null && resp.getHits() != null && resp.getHits().getHits() != null && resp.getHits().getHits().length == 1) {
                    SearchHit hit = resp.getHits().getHits()[0];
                    ret = hit.getSourceAsString();
//                    System.out.println("busqueda:......\n"+ret);
                    DataObject jsonObj = (DataObject) DataObject.parseJSON(ret);
                    ret = jsonObj.getString("culturaoaiid", null);
                }

            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
            return ret;
        }

        private String getId(SearchResponse resp) {
            String ret = null;
            if (resp != null && resp.getHits() != null
                    && resp.getHits().getHits() != null && resp.getHits().getHits().length == 1) {
                SearchHit hit = resp.getHits().getHits()[0];
                ret = hit.getSourceAsString();
            }
            return ret;
        }

        /**
         * Gets an object from ElasticSearch using document identifier.
         *
         * @param id Identifier of document to retrieve from index.
         * @return JSONObject wrapping document information.
         */
        private static JSONObject getObjectById(String id) {
            JSONObject ret = null;
            GetRequest req = new GetRequest(REPO_INDEX, REPO_TYPE, id);
            try {
                GetResponse response = getElasticClient().get(req);
                if (response.isExists()) {
                    ret = new JSONObject(response.getSourceAsString());
                    ret.put("_id", response.getId());
                }
            } catch (IOException ioex) {
                logger.error(ioex);
            }

            return ret;
        }

        /**
         * Indexes a list of objects in ElasticSearch using bulk API.
         *
         * @param objects List of objects Strings in JSON format.
         * @param client {@link RestHighLevelClient} object.
         * @param indexName Name of index to use.
         * @param typeName Name of type in index.
         * @return List of identifiers of indexed objects.
         */
        public static ArrayList<String> indexObjects(RestHighLevelClient client, String indexName, String typeName, ArrayList<String> objects) {
            ArrayList<String> ret = new ArrayList<>();
            BulkRequest request = new BulkRequest();

            for (String obj : objects) {
                String id = ELASTICSEARCH.getUUID();
                IndexRequest req = new IndexRequest(indexName, typeName, id);
                req.source(obj, XContentType.JSON);
                request.add(req);

                try {
                    BulkResponse resp = client.bulk(request);
                    for (BulkItemResponse itemResponse : resp) {
                        DocWriteResponse r = itemResponse.getResponse();
                        if (itemResponse.getOpType() == DocWriteRequest.OpType.INDEX || itemResponse.getOpType() == DocWriteRequest.OpType.CREATE) {
                            IndexResponse indexResponse = (IndexResponse) r;
                            if (indexResponse.status().getStatus() == RestStatus.CREATED.getStatus()
                                    || indexResponse.status().getStatus() == RestStatus.OK.getStatus()) {
                                ret.add(indexResponse.getId());
                            }
                        }
                    }
                } catch (IOException ioex) {
                    logger.error("Error indexing objects in bulk request", ioex);
                }
            }
            return ret;
        }

        /**
         *
         * @param client {@link RestHighLevelClient} object.
         * @param indexName Index name
         * @param typeName Property type name
         * @param oId Object identifier
         * @param json Object with updated information
         * @return Id of the updated object
         */
        public static String updateObject(RestHighLevelClient client, String indexName, String typeName, String oId, String json) {
            String ret = "";
            if (null != oId && !oId.isEmpty()) {
                UpdateRequest req = new UpdateRequest(indexName, typeName, oId);
                req.doc(json, XContentType.JSON);

                try {
                    UpdateResponse resp = client.update(req);
                    if (resp.getResult() == DocWriteResponse.Result.UPDATED) {
                        ret = resp.getId();
                    }
                } catch (IOException ioex) {
                    logger.error(ioex);
                }
            }

            return ret;
        }

        /**
         * Deletes objects by holder name
         *
         * @param holder Holder's name
         * @return number of deleted records
         */
//        public static long deleteObjectsByHolder(String holder) {
//            long deleted = -1;
//            try {
//                System.out.println("deleteObjectsByHolder");
//                TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
//                        .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300));
//                BulkByScrollResponse response = DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
//                        .filter(QueryBuilders.matchQuery("holder", holder))
//                        .source("bic")
//                        .get();
//                deleted = response.getDeleted();
//            } catch (UnknownHostException uhe) {
//                logger.error(uhe);
//                System.out.println(uhe.toString());
//            }
//            System.out.println(deleted);
//            return deleted;
//        }
        /**
         * Gets index name to work with according to environment configuration.
         *
         * @return Name of index to use.
         */
        public static String getIndexName() {
            return Util.ENV_DEVELOPMENT.equals(Util.getEnvironmentName()) ? REPO_INDEX_TEST : REPO_INDEX;
        }

        /**
         * Creates an index in ElasticSearch.
         *
         * @param client {@link RestHighLevelClient} object.
         * @param indexName Name of index to use.
         * @param mapping JSON String of index mapping.
         * @return true if index is created, false otherwise.
         */
        public static boolean createIndex(RestHighLevelClient client, String indexName, String mapping) {
            boolean ret = false;
            HttpEntity body = new NStringEntity(mapping, ContentType.APPLICATION_JSON);
            HashMap<String, String> params = new HashMap<>();

            try {
                Response resp = client.getLowLevelClient().performRequest("PUT", "/" + indexName, params, body);
                ret = resp.getStatusLine().getStatusCode() == RestStatus.OK.getStatus();
            } catch (IOException ioex) {
                logger.error("Error creating index " + indexName, ioex);
            }
            return ret;
        }

    }

    /**
     * Inner class to encapsulate methods related to MongoDB actions.
     */
    public static final class MONGODB {

        public static String MONGO_HOST = AppConfig.getConfigObject().getMongoHost();
        public static int MONGO_PORT = AppConfig.getConfigObject().getMongoPort();
        public static String MONGO_USER = AppConfig.getConfigObject().getMongoUser();
        public static String MONGO_PASS = AppConfig.getConfigObject().getMongoPass();

        private static final HashMap<String, MongoClient> mongoClients = new HashMap<>();

        /**
         * Gets a {@link MongoClient} instance with default host and port.
         *
         * @return MongoClient instance object.
         */
        public static MongoClient getMongoClient() {
            return getMongoClient(MONGO_HOST, MONGO_PORT);
        }

        /**
         * Gets a {@link MongoClient} instance with given host and port.
         *
         * @param host MongoDB server host.
         * @param port MongoDB server port.
         * @return MongoClient instance object.
         */
        public static MongoClient getMongoClient(String host, int port) {
            MongoClient ret = mongoClients.get(host + ":" + String.valueOf(port));
            if (null == ret) {
                ret = new MongoClient(host, port);
                mongoClients.put(host + ":" + String.valueOf(port), ret);
            }

            return ret;
        }
    }

    /**
     * Inner class to encapsulate methods related to SWBForms DataManager
     * actions.
     */
    public static final class SWBForms {

        /**
         * Carga en el hm todas las propiedades del DataObject
         *
         * @param prop DataObject a revisar para obtener la lista de propiedades
         * existentes
         * @param propName Nombre de la propiedad actual que se revisa
         */
        public static void findProps(DataObject prop, String propName, HashMap<String, String> collection, SWBScriptEngine engine) {
            Iterator<String> it = prop.keySet().iterator();
            while (it.hasNext()) {
                String next = it.next();
                Object obj = prop.get(next);
                String key = propName + "." + next;
                if (null != obj && obj instanceof DataObject) {
                    findProps((DataObject) obj, key, collection, engine);
                } else {
                    //buscar propiedad en el hashmap
                    if (collection.get(propName) != null) {
                        // Sólo puede tener un valor
                        String coll2use = collection.get(propName);
                        SWBDataSource dscoll = engine.getDataSource(coll2use);

                        if (null != dscoll) {
                            try {
                                DataObject r = new DataObject();
                                DataObject data = new DataObject();
                                r.put("data", data);
                                data.put("value", obj);
                                DataObject ret = dscoll.fetch(r);
                                DataList rdata = ret.getDataObject("response").getDataList("data");
                                DataObject res;
                                if (!rdata.isEmpty()) {
                                    StringBuilder replaceValue = new StringBuilder();
                                    Iterator<DataObject> dit = rdata.iterator();
                                    while (dit.hasNext()) {
                                        res = dit.next();
                                        replaceValue.append(res.getString("replace"));
                                        if (dit.hasNext()) {
                                            replaceValue.append(",");
                                        }
                                    }
                                    prop.put(next, replaceValue.toString());
                                } else {
                                    logger.debug("No se encontró valor " + obj + " en la colección..." + coll2use);
                                }
                            } catch (Exception e) {
                                //No se encontró la propiedad con el valor actual
                                logger.error("Error al buscar el valor en la colección", e);
                            }
                        }
                    }
                }
            }
        }

        /**
         * MApeo de propiedades definidas en el DataObject transformado
         *
         * @param dobj DataObject para revisar propiedades y mapearlas en
         * relación a la tabla de mapeo definida en el mismo extractor
         * @param collection Tabla de mapeo cargada en un HashMap<String,String>
         * definida en el extractor
         * @param engine, para obtener el dataSource de la colección en donde se
         * buscará el valor a sustituir, si existe.
         */
        public static void findProps(DataObject dobj, HashMap<String, String> collection, SWBScriptEngine engine) {

            Iterator<String> it = dobj.keySet().iterator();
            while (it.hasNext()) {
                String next1 = it.next();

                Object obj = dobj.get(next1);
                if (null != obj && obj instanceof DataObject) {
                    findProps((DataObject) obj, next1, collection, engine);
                } else {
                    //buscar y actualizar propiedad
                    //buscar propiedad en el hashmap
//                System.out.println("Buscando propiedad en HM "+next1);
                    next1 = next1.trim();
                    if (collection.get(next1) != null) {
                        // Sólo puede tener un valor
                        String coll2use = collection.get(next1);
                        SWBDataSource dscoll = engine.getDataSource(coll2use);

                        if (null != dscoll) {
                            try {
                                DataObject r = new DataObject();
                                DataObject data = new DataObject();
                                r.put("data", data);
                                data.put("value", obj);
                                DataObject ret = dscoll.fetch(r);
                                DataList rdata = ret.getDataObject("response").getDataList("data");
                                DataObject res;
                                if (!rdata.isEmpty()) {
                                    StringBuilder replaceValue = null;
                                    for (int i = 0; i < rdata.size(); i++) {
                                        res = rdata.getDataObject(i);  // DataObject de Replace
                                        if (replaceValue == null) {
                                            replaceValue = new StringBuilder();
                                        }
                                        if (replaceValue.length() > 0) {
                                            replaceValue.append(",");
                                        }
                                        replaceValue.append(res.getString("replace"));
                                    }
                                    dobj.put(next1, replaceValue.toString());
                                } else {
                                    logger.debug("No se encontró valor " + obj + " en la colección..." + coll2use);
                                }
                            } catch (Exception e) {
                                //No se encontró la propiedad con el valor actual
                                logger.error("Error al buscar el valor en la colección", e);
                            }

                        }
                    }
                    //hm.put(next1, next1);
                }
            }
//        return hm;
        }

        /**
         * Carga la colección de Replace a un HashMap<ocurrencia, reemplazo>
         *
         * @param engine Utilizado para poder cargar la colección de Replace en
         * un HashMap
         * @return HashMap con DataSource cargado en memoria.
         */
        public static HashMap<String, String> loadExtractorMapTable(SWBScriptEngine engine, DataObject extDef) {
            HashMap<String, String> hm = new HashMap();
            try {
                SWBDataSource dsMDef = engine.getDataSource("MapDefinition");
                DataObject doMDef = dsMDef.fetchObjById(extDef.getString("mapDef"));
                SWBDataSource ds = engine.getDataSource("MapTable");

                try {
                    DataList dl = doMDef.getDataList("mapTable");
                    if (null != dl && dl.size() > 0) {
                        //System.out.println("MapTable");
                        for (int i = 0; i < dl.size(); i++) {
                            String llave = dl.getString(i);
                            DataObject dobj = ds.fetchObjById(llave);
                            if (null != dobj) {
                                //System.out.println("("+dobj.getString("property")+","+dobj.getString("collName")+")");
                                hm.put(dobj.getString("property"), dobj.getString("collName"));
                            }
                        }
                    }

                } catch (Exception e) {
                    logger.error("Error al cargar el DataSource. ", e);
                }
            } catch (Exception ex) {
                logger.error(ex);
            }

            return hm;
        }

        /**
         * Carga la colección de Replace a un HashMap<ocurrencia, reemplazo>
         *
         * @param engine Utilizado para poder cargar la colección de Replace en
         * un HashMap
         * @return HashMap con DataSource cargado en memoria.
         */
        public static HashMap<String, String> loadOccurrences(SWBScriptEngine engine) {

            SWBDataSource datasource;
            HashMap<String, String> hm = new HashMap();

            if (null != engine) {
                try {
                    datasource = engine.getDataSource("Replace");
                    DataObject r = new DataObject();
                    DataObject data = new DataObject();
                    r.put("data", data);

                    DataObject ret = datasource.fetch(r);
                    String occurrence;
                    String replace;

                    DataList rdata = ret.getDataObject("response").getDataList("data");
                    DataObject dobj;
                    if (!rdata.isEmpty()) {
                        for (int i = 0; i < rdata.size(); i++) {
                            dobj = rdata.getDataObject(i);  // DataObject de Replace
                            occurrence = dobj.getString("occurrence");
                            replace = dobj.getString("replace");
                            hm.put(occurrence, replace);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error al cargar el DataSource. ", e);
                }
            } else {
                logger.error("Error al cargar el DataSource al HashMap, falta inicializar el engine.");
                return null;
            }

            return hm;
        }

        /**
         * Converts a {@link BasicDBObject} into a MongoDB
         * {@link com.mongodb.DBObject}
         *
         * @param obj {@link DataObject} to transform
         * @return BasicDBObject
         */
        public static BasicDBObject toBasicDBObject(DataObject obj) {
            BasicDBObject ret = new BasicDBObject();
            Iterator<Map.Entry<String, Object>> it = obj.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                ret.put(entry.getKey(), toBasicDB(entry.getValue()));
            }
            return ret;
        }

        /**
         * Converts an Object into a MongoDB generic object.
         *
         * @param obj {@link Object} to convert
         * @return MongoDB generic object
         */
        public static Object toBasicDB(Object obj) {
            if (obj instanceof DataObject) {
                return toBasicDBObject((DataObject) obj);
            } else if (obj instanceof DataList) {
                return toBasicDBList((DataList) obj);
            }
            return obj;
        }

        /**
         * Converts a {@link DataList} into a MongoDB {@link BasicDBList}
         *
         * @param obj {@link DataList} to convert
         * @return BasicDBList
         */
        public static BasicDBList toBasicDBList(DataList obj) {
            BasicDBList ret = new BasicDBList();
            Iterator it = obj.iterator();
            while (it.hasNext()) {
                ret.add(toBasicDB(it.next()));
            }
            return ret;
        }
    }

    /**
     * Inner class to encapsulate methods related with File manipulation
     */
    public static final class FILE {

        /**
         * Reads {@link java.io.InputStream} content as a {@link String}
         *
         * @param fis {@link FileInputStream} to read from
         * @param encoding Name of encoding to use on content read.
         * @return String wirh {@link InputStream} content.
         */
        public static String readFromStream(InputStream fis, String encoding) {
            StringBuilder ret = new StringBuilder();
            String enc = StandardCharsets.UTF_8.name();

            if (null != encoding && !enc.isEmpty()) {
                enc = encoding;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(fis, enc))) {
                String line;
                while ((line = br.readLine()) != null) {
                    ret.append(line);
                }
            } catch (IOException ioex) {
                logger.error("Error reading file", ioex);
            }

            return ret.toString();
        }

    }

    /**
     * Inner class to encapsulate methods related with String manipulation
     */
    public static final class TEXT {

        public static String toStringHtmlEscape(String str) {
            StringBuilder buf = new StringBuilder();
            int c = 0;
            int i = str.indexOf("\\u", c);
            while (i > -1) {
                buf.append(str.substring(c, i));
                int v = Integer.parseInt(str.substring(i + 2, i + 6), 16);
                buf.append("&#" + v + ";");
                c = i + 6;
                i = str.indexOf("\\u", c);
            }
            buf.append(str.substring(c));
            return buf.toString();
        }

        /**
         * Format milliseconds long number in days, hours, minutes, seconds and
         * milliseconds
         *
         * @param elapsedTime time to determinate in days, hours, minutes,
         * seconds and milliseconds takes.
         * @return a text in days, hours, minutes, seconds and milliseconds
         * format
         */
        public static String getElapsedTime(long elapsedTime) {
            String etime;
            long seg = 1000;
            long min = 60 * seg;
            long hr = 60 * min;
            long day = 24 * hr;

            long ndays = 0;
            long nhr = 0;
            long nmin = 0;
            long nseg = 0;
            long nms = 0;

            if (elapsedTime > day) {
                ndays = elapsedTime / day;
                if (ndays > 0 && (elapsedTime % day) > 0) {
                    nhr = ((elapsedTime % day)) / hr;
                }
                if (nhr > 0 && (((elapsedTime % day)) % hr) > 0) {
                    nmin = ((elapsedTime % day) % hr) / min;
                }
                if (nmin > 0 && (((elapsedTime % day) % hr) % min) > 0) {
                    nseg = (((elapsedTime % day) % hr) % min) / seg;
                }
                if (nseg > 0 && ((((elapsedTime % day) % hr) % min) % seg) > 0) {
                    nms = (((elapsedTime % day) % hr) % min) % seg;
                }

            } else if (elapsedTime > hr) {
                nhr = elapsedTime / hr;
                if (nhr > 0 && (elapsedTime % hr) > 0) {
                    nmin = (elapsedTime % hr) / min;
                }
                if (nmin > 0 && ((elapsedTime % hr) % min) > 0) {
                    nseg = ((elapsedTime % hr) % min) / seg;
                }
                if (nseg > 0 && (((elapsedTime % hr) % min) % seg) > 0) {
                    nms = (((elapsedTime % hr) % min) % seg);
                }
            } else if (elapsedTime > min) {
                nmin = elapsedTime / min;
                if (nmin > 0 && (elapsedTime % min) > 0) {
                    nseg = ((elapsedTime % min)) / seg;
                }
                if (nseg > 0 && (((elapsedTime % min) % seg)) > 0) {
                    nms = ((elapsedTime % min) % seg);
                }

            } else if (elapsedTime > seg) {
                nseg = elapsedTime / seg;
            } else {
                nms = elapsedTime;
            }

            etime = "";
            if (ndays > 0) {
                etime = ndays + "dias ";
            }
            if (nhr > 0) {
                if (nhr < 10) {
                    etime += "0" + nhr + "hrs ";
                } else {
                    etime += nhr + "hrs ";
                }
            }
            if (nmin > 0) {
                if (nmin < 10) {
                    etime += "0" + nmin + "min ";
                } else {
                    etime += nmin + "min ";
                }
            }
            if (nseg > 0) {
                if (nseg < 10) {
                    etime += "0" + nseg + "sec ";
                } else {
                    etime += nseg + "sec ";
                }
            }
            if (nms > 0) {
                etime += nms + "ms";
            }

            return etime;
        }

        /**
         * Reemplaza las ocurrencias en el string recibido
         *
         * @param hm HashMap con las ocurrencias y su reemplazo previamente
         * cargado
         * @param oaistr Stream del registro OAI a revisar
         * @return String con todas las ocurrencias reemplazadas.
         */
        public static String replaceOccurrences(HashMap<String, String> hm, String oaistr) {
            if (null != hm && null != oaistr) {
                String occurrence;
                String replace;
                Iterator<String> it = hm.keySet().iterator();
                while (it.hasNext()) {
                    occurrence = it.next();
                    replace = hm.get(occurrence);
                    oaistr = oaistr.replace(occurrence, replace);
                }
            }
            return oaistr;
        }

        /**
         * Replaces accented characters and blank spaces in the string given.
         * Makes the changes in a case sensitive manner, the following are some
         * examples of the changes this method makes: <br>
         *
         * @param txt a string in which the characters are going to be replaced
         * @param replaceSpaces a {@code boolean} indicating if blank spaces are
         * going to be replaced or not
         * @return a string similar to {@code txt} but with neither accented or
         * special characters nor symbols in it. un objeto string similar a
         * {@code txt} pero sin caracteres acentuados o especiales y sin
         * s&iacute;mbolos {@literal Á} is replaced by {@literal A} <br>
         * {@literal Ê} is replaced by {@literal E} <br> {@literal Ï} is
         * replaced by {@literal I} <br> {@literal â} is replaced by
         * {@literal a} <br> {@literal ç} is replaced by {@literal c} <br>
         * {@literal ñ} is replaced by {@literal n} <br>
         * and blank spaces are replaced by underscore characters, any symbol in
         * {@code txt} other than underscore is eliminated including the
         * periods.
         * <p>
         * Reemplaza caracteres acentuados y espacios en blanco en {@code txt}.
         * Realiza los cambios respetando caracteres en may&uacute;sculas o
         * min&uacute;sculas los caracteres en blanco son reemplazados por
         * guiones bajos, cualquier s&iacute;mbolo diferente a gui&oacute;n bajo
         * es eliminado.</p>
         */
        public static String replaceSpecialCharacters(String txt, boolean replaceSpaces) {
            StringBuffer ret = new StringBuffer();
            String aux = txt;
            //aux = aux.toLowerCase();
            aux = aux.replace('Á', 'A');
            aux = aux.replace('Ä', 'A');
            aux = aux.replace('Å', 'A');
            aux = aux.replace('Â', 'A');
            aux = aux.replace('À', 'A');
            aux = aux.replace('Ã', 'A');

            aux = aux.replace('É', 'E');
            aux = aux.replace('Ê', 'E');
            aux = aux.replace('È', 'E');
            aux = aux.replace('Ë', 'E');

            aux = aux.replace('Í', 'I');
            aux = aux.replace('Î', 'I');
            aux = aux.replace('Ï', 'I');
            aux = aux.replace('Ì', 'I');

            aux = aux.replace('Ó', 'O');
            aux = aux.replace('Ö', 'O');
            aux = aux.replace('Ô', 'O');
            aux = aux.replace('Ò', 'O');
            aux = aux.replace('Õ', 'O');

            aux = aux.replace('Ú', 'U');
            aux = aux.replace('Ü', 'U');
            aux = aux.replace('Û', 'U');
            aux = aux.replace('Ù', 'U');

            aux = aux.replace('Ñ', 'N');

            aux = aux.replace('Ç', 'C');
            aux = aux.replace('Ý', 'Y');

            aux = aux.replace('á', 'a');
            aux = aux.replace('à', 'a');
            aux = aux.replace('ã', 'a');
            aux = aux.replace('â', 'a');
            aux = aux.replace('ä', 'a');
            aux = aux.replace('å', 'a');

            aux = aux.replace('é', 'e');
            aux = aux.replace('è', 'e');
            aux = aux.replace('ê', 'e');
            aux = aux.replace('ë', 'e');

            aux = aux.replace('í', 'i');
            aux = aux.replace('ì', 'i');
            aux = aux.replace('î', 'i');
            aux = aux.replace('ï', 'i');

            aux = aux.replace('ó', 'o');
            aux = aux.replace('ò', 'o');
            aux = aux.replace('ô', 'o');
            aux = aux.replace('ö', 'o');

            aux = aux.replace('ú', 'u');
            aux = aux.replace('ù', 'u');
            aux = aux.replace('ü', 'u');
            aux = aux.replace('û', 'u');

            aux = aux.replace('ñ', 'n');

            aux = aux.replace('ç', 'c');
            aux = aux.replace('ÿ', 'y');
            aux = aux.replace('ý', 'y');

            if (replaceSpaces) {
                aux = aux.replace(' ', '_');
            }
            int l = aux.length();
            for (int x = 0; x < l; x++) {
                char ch = aux.charAt(x);
                if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'z')
                        || (ch >= 'A' && ch <= 'Z') || ch == '_' || ch == '-') {
                    ret.append(ch);
                }
            }
            aux = ret.toString();
            return aux;
        }
    }

    /**
     * Genera el Id consecutivo por Proveedor de datos
     *
     * @param holderIdentifier
     * @param engine
     * @return Identificador numérico consecutivo por proveedor de datos
     */
    public static synchronized long nextHolderId(String holderIdentifier, SWBScriptEngine engine) {
        long next = -1L;

        DataObject dobj;
        SWBDataSource ds = engine.getDataSource("Serial");
        dobj = new DataObject();
        dobj.addSubObject("data").addParam("holderid", holderIdentifier);
        try {
            dobj = ds.fetch(dobj);
            if (null != dobj && dobj.getDataObject("response").getDataList("data").size() == 1) {
                dobj = dobj.getDataObject("response").getDataList("data").getDataObject(0);
                next = dobj.getLong("next");
                next++;
                dobj.put("next", next);
                ds.updateObj(dobj);
            } else {
                DataObject data = new DataObject();
                data.put("holderid", holderIdentifier);
                data.put("next", 1);
                next = 1;
                DataObject ret = ds.addObj(data);
            }
        } catch (IOException ioe) {
            System.out.println("Error al generar Id consecutivo por proveedor de datos. Util.nextHolderId()");
        }
        return next;
    }

    /**
     * Regresa la etiqueta de la propiedad en el lenguaje solicitado, (es o en)
     * si no se encuentra en el lenguaje, regresa el valor en el otro lenguaje
     * si existe de lo contrario regresa null.
     *
     * @param propertyName, nombre de la propiedad
     * @param language, código del lenguaje a buscar (es o en)
     * @return Etiqueta de la propiedad en el lenguaje, null si no existe la
     * propiedad
     */
    public static String getPropertyLabel(String propertyName, String language) {
        String ret = null;
        HashMap<String, String> hmlangs = null;
        if (null == hmproplbl) {
            hmproplbl = new HashMap();
            DataObject dobj;
            try {
                DB db = Util.MONGODB.getMongoClient().getDB("SWBForms");
                DBCollection dsLbls = db.getCollection("propLbls");
                DBCursor cursor = dsLbls.find();
                while (null != cursor && cursor.hasNext()) {
                    DBObject next = cursor.next();
                    dobj = (DataObject) DataObject.parseJSON(next.toString());
                    hmlangs = new HashMap();
                    hmlangs.put("es", dobj.getString("es", null));
                    hmlangs.put("en", dobj.getString("en", null));
                    hmproplbl.put(dobj.getString("propid"), hmlangs);
                }
            } catch (Exception e) {
                System.out.println("Error al buscar etiqueta de la propiedad (" + propertyName + "). Util.getPropertyLabel()");
                e.printStackTrace(System.out);
            }
        } else {
            if (hmproplbl.containsKey(propertyName)) {
                hmlangs = hmproplbl.get(propertyName);
                if (hmlangs.containsKey(language)) {
                    if (hmlangs.get(language) != null) {
                        ret = hmlangs.get(language);
                    } else {
                        ret = hmlangs.get("es"); // por defecto regresa la etiqueta en español, null si no existe.
                    }
                } else {
                    ret = null;
                }
            }
        }
        return ret;
    }

    /**
     * Gets from DataSource dsName the list of properties if this dsName exists
     * in the DataSource Collection
     *
     * @param dsName DataSource Name
     * @return HashMap<propertyName,Property DataObject> with a list of
     * propertyId - property DataObject
     */
    public static HashMap<String, DataObject> getAllDSProps(String dsName) {

        HashMap<String, DataObject> ret = new HashMap();
        try {
            DB db = Util.MONGODB.getMongoClient().getDB("SWBForms");
            DBCollection datasource = db.getCollection("DataSource");
            DBCollection datafields = db.getCollection("DataSourceFields");

            BasicDBObject dbQuery = new BasicDBObject("id", dsName);
            DBObject dores = datasource.findOne(dbQuery);
            DataObject dods = (DataObject) DataObject.parseJSON(dores.toString());

            String dsid = dods.getId();

            BasicDBObject dbQueryProp = new BasicDBObject("ds", dsid);
            DBCursor cursor = datafields.find(dbQueryProp);
            BasicDBObject sortBy = new BasicDBObject("order", 1);
            cursor.sort(sortBy);

            while (null != cursor && cursor.hasNext()) {
                DBObject next = cursor.next();
                DataObject dobj = (DataObject) DataObject.parseJSON(next.toString());
                String objType = dobj.getString("type");
                if (objType != null && !objType.isEmpty() && !objType.equals("section") && !objType.equals("header")) {
                    ret.put(dobj.getString("name"), dobj);
                }
            }
            cursor.close();
        } catch (Exception e) {
            System.out.println("Error al obtener la lista de propiedades del DataSource(" + dsName + "). Util.getAllDSProps()\n\n");
            e.printStackTrace(System.out);
        }
        return ret;
    }

    /**
     * Gets from DataSource dsName the list of facet properties if this dsName
     * exists in the DataSource Collection
     *
     * @param dsName DataSource Name
     * @return HashMap<propertyName,Property DataObject> with a list of
     * propertyId - property DataObject
     */
    public static HashMap<String, DataObject> getAllDSFacetProps(String dsName) {

        HashMap<String, DataObject> ret = new HashMap();
        HashMap<String, DataObject> hmAll = getAllDSProps(dsName);
        Iterator<DataObject> itprops = hmAll.values().iterator();
        while (itprops.hasNext()) {
            DataObject dobj = itprops.next();
            try {
                if (dobj.getBoolean("facetado")) {
                    ret.put(dobj.getString("name"), dobj);
                }
            } catch (Exception e) {
                System.out.println("Error al obtener la lista de propiedades para el facetado del DataSource(" + dsName + "). Util.getAllDSProps()");
            }
        }
        return ret;
    }

    /**
     * Gets from DataSource dsName the list of visible properties if this dsName
     * exists in the DataSource Collection
     *
     * @param dsName DataSource Name
     * @return HashMap<propertyName,Property DataObject> with a list of
     * propertyId - property DataObject
     */
    public static HashMap<String, DataObject> getAllDSVisibleProps(String dsName) {

        HashMap<String, DataObject> ret = new HashMap();
        HashMap<String, DataObject> hmAll = getAllDSProps(dsName);
        Iterator<DataObject> itprops = hmAll.values().iterator();
        while (itprops.hasNext()) {
            DataObject dobj = itprops.next();
            try {
                if (dobj.getBoolean("visible")) {
                    ret.put(dobj.getString("name"), dobj);
                }
            } catch (Exception e) {
                System.out.println("Error al obtener la lista de propiedades visibles del DataSource(" + dsName + "). Util.getAllDSProps()");
            }
        }
        return ret;
    }

    /**
     * Regresa el DataObject con el OAIID generado
     *
     * @param jsonObj JSON con la información del objeto que se le agregará el
     * OAIID
     * @param extractor DataObject de la definición del Extractor
     * @param engine
     * @param transobjs colección en donde se actualizará el JSON
     * @return el DataObject actualizado con el OAIID generado
     */
    public static synchronized DataObject addPatternOAIID2DataObject(String jsonObj, DataObject extractor, SWBScriptEngine engine, SWBDataSource transobjs) {
        //boolean ret = false;
        DataObject dobj = null;
        if (null != extractor && null != jsonObj) {
            try {
                dobj = (DataObject) DataObject.parseJSON(jsonObj);

                String hldrId = extractor.getString("holderid");
                if (null != hldrId && hldrId.startsWith("NI")) {
                    hldrId = hldrId.substring(2);
                }
//                if (dobj.getString(" ", null) != null) {
//                    return dobj;
//                }
                String oaiPattern = AppConfig.getConfigObject().getOAIPattern();
//                System.out.println("\nPattern: "+oaiPattern+"\n\n");
                String culturaId = oaiPattern;
                String hldrIdDO = null;
                String hldrOrig = null;
                if (dobj.getString("holderid", null) != null && dobj.getString("holderid").trim().length() > 0) {
                    hldrIdDO = dobj.getString("holderid").trim();
                    if (hldrIdDO.toUpperCase().startsWith("NI")) {
                        hldrIdDO = hldrIdDO.substring(2);
                    }
                }
                if (null != hldrIdDO) {
                    culturaId = oaiPattern.replace("{@idHolder}", hldrIdDO);
                    hldrOrig = "NI" + hldrIdDO;
                } else if (hldrId != null) {
                    culturaId = oaiPattern.replace("{@idHolder}", hldrId);
                    hldrOrig = "NI" + hldrId;
                } else {
                    return dobj;
                }
                String culturaoaiid = culturaId + String.format("%07d", Util.nextHolderId(hldrOrig, engine));
                dobj.put("culturaoaiid", culturaoaiid);
                transobjs.updateObj(dobj);
            } catch (Exception e) {
                System.out.println("Error al generar el OAIID de acuerdo al Pattern configurado. Util.addPatternOAIID2DataObject()");
            }

        }
        return dobj;
    }

}
