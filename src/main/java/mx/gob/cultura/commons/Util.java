package mx.gob.cultura.commons;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
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
import java.util.Iterator;
import java.util.Map;

/**
 * Utility class with common methods.
 *
 * @author Hasdai Pacheco
 */
public final class Util {
    private static final Logger LOGGER = Logger.getLogger(Util.class);
    public static final String ENV_DEVELOPMENT = "DEV";
    public static final String ENV_TESTING = "TEST";
    public static final String ENV_QA = "QA";
    public static final String ENV_PRODUCTION = "PROD";

    private Util() { }

    public static String makeRequest(URL theUrl, boolean xmlSupport) {
        HttpURLConnection con = null;
        StringBuilder response = new StringBuilder();
        String errorMsg = null;
        int retries = 0;
        boolean isConnOk;

        do {
            LOGGER.trace("Trying to make request to URL " + theUrl.toString());
            try {
                con = (HttpURLConnection) theUrl.openConnection();
                isConnOk = true;
                //AQUI SE PIDE EN XML
                if (xmlSupport) {
                    LOGGER.trace("Setting XML request header");
                    con.setRequestProperty("accept", "application/xml");
                }
                con.setRequestMethod("GET");
                int statusCode = con.getResponseCode();

                if (statusCode == 200) {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                        String inputLine;

                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                    } catch (IOException ioex) {
                        LOGGER.error(ioex);
                    }
                } else {
                    LOGGER.debug("Server responded " + statusCode +" for " + theUrl);
                }
            } catch (IOException e) {
                LOGGER.trace("Failed connection to URL "+theUrl+". Retrying");
                if (null != con) {
                    con.disconnect();
                }
                retries++;
                try {
                    Thread.sleep(5000);
                } catch (Exception te) {
                    LOGGER.error(te);
                }

                isConnOk = false;
                if(retries==5){
                    LOGGER.trace("Max number of retries reached ("+retries+")");
                    errorMsg="#Error: No se puede conectar al servidor#";
                }
            }
        } while (!isConnOk && retries < 5);
        return errorMsg!=null?errorMsg:response.toString();
    }

    /**
     * Gets environment configuration.
     * @return Value of REPO_DEVENV environment property. Defaults to production.
     */
    public static String getEnvironmentName() {
        String env = System.getenv("REPO_DEVENV");
        if (null == env) env = ENV_PRODUCTION;
        return env;
    }

    /**
     * Inner class to encapsulate methods related to ElasticSearch actions.
     */
    public static final class ELASTICSEARCH {
        public static final String REPO_INDEX = "cultura";
        public static final String REPO_INDEX_TEST = "cultura_test";
        private static final HashMap<String, RestHighLevelClient> elasticClients = new HashMap<>();

        private ELASTICSEARCH() {}

        /**
         * Gets a {@link RestHighLevelClient} instance with default host and port.
         *
         * @return RestHighLevelClient instance object.
         */
        public static RestHighLevelClient getElasticClient() {
            return getElasticClient("localhost", 9200);
        }

        /**
         * Gets a {@link RestHighLevelClient} instance with given host and port.
         *
         * @param host ElasticSearch node host name.
         * @param port ElasticSearch node port.
         * @return RestHighLevelClient instance object.
         */
        public static RestHighLevelClient getElasticClient(String host, int port) {
            RestHighLevelClient ret = elasticClients.get(host + ":" + port);
            if (null == ret) {
                ret = new RestHighLevelClient(
                        RestClient.builder(new HttpHost(host, port)));

                elasticClients.put(host + ":" + port, ret);
            }
            return ret;
        }

        /**
         * Closes an ElasticSearch {@link RestHighLevelClient} associated with @host and @port.
         * @param host Hostname of client
         * @param port Port number of client
         */
        public static void closeElasticClient(String host, int port) {
            RestHighLevelClient ret = elasticClients.get(host + ":" + port);
            if (null != ret) {
                try {
                    ret.close();
                    elasticClients.remove(host + ":" + port, ret);
                } catch (IOException ioex) {
                    LOGGER.error("Error while closing ES client", ioex);
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
                    LOGGER.error("Error while closing ES clients", ioex);
                }
            }
        }

        /**
         * Gets time-based UUID for indexing objects.
         * @return String representation of a time-based UUID.
         */
        public static String getUUID() {
            return UUIDs.base64UUID();
        }

        /**
         * Indexes an object in ElasticSearch.
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
                if (resp.status().getStatus() == RestStatus.CREATED.getStatus() ||
                        resp.status().getStatus() == RestStatus.OK.getStatus()) {
                    ret = resp.getId();
                }
            } catch (IOException ioex) {
                LOGGER.error("Error making index request for object with id "+objectId, ioex);
            }

            return ret;
        }

        /**
         * Indexes a list of objects in ElasticSearch using bulk API.
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
                            if (indexResponse.status().getStatus() == RestStatus.CREATED.getStatus() ||
                                    indexResponse.status().getStatus() == RestStatus.OK.getStatus()) {
                                ret.add(indexResponse.getId());
                            }
                        }
                    }
                } catch (IOException ioex) {
                    LOGGER.error("Error indexing objects in bulk request", ioex);
                }
            }
            return ret;
        }

        /**
         * Gets index name to work with according to environment configuration.
         * @return Name of index to use.
         */
        public static String getIndexName() {
            return Util.ENV_DEVELOPMENT.equals(Util.getEnvironmentName()) ? REPO_INDEX_TEST : REPO_INDEX;
        }

        /**
         * Creates an index in ElasticSearch.
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
                Response resp = client.getLowLevelClient().performRequest("PUT", "/"+ indexName, params, body);
                ret = resp.getStatusLine().getStatusCode() == RestStatus.OK.getStatus();
            } catch (IOException ioex) {
                LOGGER.error("Error creating index "+indexName, ioex);
            }
            return ret;
        }
    }

    /**
     * Inner class to encapsulate methods related to MongoDB actions.
     */
    public static final class MONGODB {
        private static final HashMap<String, MongoClient> mongoClients = new HashMap<>();

        private MONGODB () {}

        /**
         * Gets a {@link MongoClient} instance with default host and port.
         *
         * @return MongoClient instance object.
         */
        public static MongoClient getMongoClient() {
            return getMongoClient("localhost", 27017);
        }

        /**
         * Gets a {@link MongoClient} instance with given host and port.
         *
         * @param host MongoDB server host.
         * @param port MongoDB server port.
         * @return MongoClient instance object.
         */
        public static MongoClient getMongoClient(String host, int port) {
            MongoClient ret = mongoClients.get(host + ":" + port);
            if (null == ret) {
                ret = new MongoClient(host, port);
                mongoClients.put(host + ":" + port, ret);
            }

            return ret;
        }
    }

    /**
     * Inner class to encapsulate methods related to SWBForms DataManager actions.
     */
    public static final class SWBForms {

        private SWBForms () {}

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
                                    LOGGER.debug("No se encontró valor " + obj + " en la colección..." + coll2use);
                                }
                            } catch (Exception e) {
                                //No se encontró la propiedad con el valor actual
                                LOGGER.error("Error al buscar el valor en la colección", e);
                            }
                        }
                    }
                }
            }
        }

        /**
         * MApeo de propiedades definidas en el DataObject transformado
         *
         * @param dobj DataObject para revisar propiedades y mapearlas en relación a
         * la tabla de mapeo definida en el mismo extractor
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
                                    StringBuilder replaceValue = new StringBuilder();
                                    for (int i = 0; i < rdata.size(); i++) {
                                        res = rdata.getDataObject(i);  // DataObject de Replace
                                        if (replaceValue.length() > 0) {
                                            replaceValue.append(",");
                                        }
                                        replaceValue.append(res.getString("replace"));
                                    }
                                    dobj.put(next1, replaceValue.toString());
                                } else {
                                    LOGGER.debug("No se encontró valor " + obj + " en la colección..." + coll2use);
                                }
                            } catch (Exception e) {
                                //No se encontró la propiedad con el valor actual
                                LOGGER.error("Error al buscar el valor en la colección", e);
                            }

                        }
                    }
                }
            }
        }

        /**
         * Carga la colección de Replace a un HashMap<ocurrencia, reemplazo>
         *
         * @param engine Utilizado para poder cargar la colección de Replace en un
         * HashMap
         * @return HashMap con DataSource cargado en memoria.
         */
        public static HashMap<String, String> loadExtractorMapTable(SWBScriptEngine engine, DataObject extDef) {
            HashMap<String, String> hm = new HashMap<>();
            SWBDataSource ds = engine.getDataSource("MapTable");
            SWBDataSource dsMDef = engine.getDataSource("MapDefinition");
            DataObject doMDef = null;

            if (null != dsMDef) {
                try {
                    doMDef = dsMDef.fetchObjById(extDef.getString("mapDef"));
                } catch (IOException e) {
                    LOGGER.error("Failed to get map definition", e);
                }

                if (null != doMDef) {
                    DataList dl = doMDef.getDataList("mapTable");
                    if (null != dl && !dl.isEmpty()) {
                        for (int i = 0; i < dl.size(); i++) {
                            String llave = dl.getString(i);
                            DataObject dobj = null;
                            try {
                                dobj = ds.fetchObjById(llave);
                            } catch (IOException e) {
                                LOGGER.error("Failed to fetch tablemap definition", e);
                            }

                            if (null != dobj) {
                                hm.put(dobj.getString("property"), dobj.getString("collName"));
                            }
                        }
                    }
                }
            }
            return hm;
        }


        /**
         * Carga la colección de Replace a un HashMap<ocurrencia, reemplazo>
         *
         * @param engine Utilizado para poder cargar la colección de Replace en un
         * HashMap
         * @return HashMap con DataSource cargado en memoria.
         */
        public static HashMap<String, String> loadOccurrences(SWBScriptEngine engine) {

            SWBDataSource datasource;
            HashMap<String, String> hm = new HashMap<>();

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
                    LOGGER.error("Error al cargar el DataSource. ", e);
                }
            } else {
                LOGGER.error("Error al cargar el DataSource al HashMap, falta inicializar el engine.");
                return null;
            }

            return hm;
        }

        /**
         * Converts a {@link BasicDBObject} into a MongoDB {@link com.mongodb.DBObject}
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

        private FILE() {}

        /**
         * Reads {@link java.io.InputStream} content as a {@link String}
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
                LOGGER.error("Error reading file", ioex);
            }

            return ret.toString();
        }
    }

    /**
     * Inner class to encapsulate methods related with String manipulation
     */
    public static final class TEXT {

        private TEXT() {}

        public static String toStringHtmlEscape(String str){
            StringBuilder buf=new StringBuilder();
            int c=0;
            int i=str.indexOf("\\u",c);
            while(i>-1)
            {
                buf.append(str.substring(c,i));
                int v=Integer.parseInt(str.substring(i+2,i+6),16);
                buf.append("&#").append(v).append(";");
                c=i+6;
                i=str.indexOf("\\u",c);
            }
            buf.append(str.substring(c));
            return buf.toString();
        }

        /**
         * Format milliseconds long number in days, hours, minutes, seconds and
         * milliseconds
         *
         * @param elapsedTime time to determinate in days, hours, minutes, seconds
         * and milliseconds takes.
         * @return a text in days, hours, minutes, seconds and milliseconds format
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
                    nhr = (elapsedTime % day) / hr;
                }
                if (nhr > 0 && ((elapsedTime % day) % hr) > 0) {
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
                    nseg = (elapsedTime % min) / seg;
                }
                if (nseg > 0 && ((elapsedTime % min) % seg) > 0) {
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
         * @param hm HashMap con las ocurrencias y su reemplazo previamente cargado
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
         * Replaces accented characters and blank spaces in the string given. Makes
         * the changes in a case sensitive manner, the following are some examples
         * of the changes this method makes: <br>
         *
         * @param txt a string in which the characters are going to be replaced
         * @param replaceSpaces a {@code boolean} indicating if blank spaces are
         * going to be replaced or not
         * @return a string similar to {@code txt} but with neither accented or
         * special characters nor symbols in it. un objeto string similar a
         * {@code txt} pero sin caracteres acentuados o especiales y sin
         * s&iacute;mbolos {@literal Á} is replaced by {@literal A} <br>
         * {@literal Ê} is replaced by {@literal E} <br> {@literal Ï} is replaced by
         * {@literal I} <br> {@literal â} is replaced by {@literal a} <br>
         * {@literal ç} is replaced by {@literal c} <br> {@literal ñ} is replaced by
         * {@literal n} <br>
         * and blank spaces are replaced by underscore characters, any symbol in
         * {@code txt} other than underscore is eliminated including the periods.
         * <p>
         * Reemplaza caracteres acentuados y espacios en blanco en {@code txt}.
         * Realiza los cambios respetando caracteres en may&uacute;sculas o
         * min&uacute;sculas los caracteres en blanco son reemplazados por guiones
         * bajos, cualquier s&iacute;mbolo diferente a gui&oacute;n bajo es
         * eliminado.</p>
         */
        public static String replaceSpecialCharacters(String txt, boolean replaceSpaces) {
            StringBuilder ret = new StringBuilder();
            String aux = txt;
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
}