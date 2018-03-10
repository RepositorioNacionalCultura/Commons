package mx.gob.cultura.test;

import mx.gob.cultura.commons.Util;
import mx.gob.cultura.commons.oai.OAIPMHURLBuilder;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;

public class URLBuilderTest {
    private static final Logger LG = Logger.getLogger(URLBuilderTest.class);
    public static void main(String []args) {
        OAIPMHURLBuilder builder = new OAIPMHURLBuilder("http://localhost:9090/");

        builder.setVerb(OAIPMHURLBuilder.VERB.LISTRECORDS);
        builder.setPrefix(OAIPMHURLBuilder.PREFIX.MEDS);
        try {
            LG.error(builder.build().toString());
            Util.makeRequest(builder.build(), true);
        } catch (MalformedURLException mfe) {
            LG.error(mfe);
        }
    }
}
