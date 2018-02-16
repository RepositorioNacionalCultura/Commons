package mx.gob.cultura.test;

import mx.gob.cultura.commons.Util;
import mx.gob.cultura.commons.oai.OAIPMHURLBuilder;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;

public class URLBuilderTest {
    static final Logger lg = Logger.getLogger(URLBuilderTest.class);
    public static void main(String []args) {
        OAIPMHURLBuilder builder = new OAIPMHURLBuilder("http://localhost:9090/");

        builder.setVerb(OAIPMHURLBuilder.VERB.ListRecords);
        builder.setPrefix(OAIPMHURLBuilder.PREFIX.meds);
        try {
            //System.out.printf(builder.build().toString());
            lg.error(builder.build().toString());
            Util.makeRequest(builder.build(), true);
        } catch (MalformedURLException mfe) {
mfe.printStackTrace();
        }
    }
}
