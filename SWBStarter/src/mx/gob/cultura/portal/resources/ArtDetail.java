/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.gob.cultura.portal.resources;

import com.google.gson.Gson;
import mx.gob.cultura.portal.response.*;
import org.semanticwb.SWBUtils;
import org.semanticwb.portal.api.GenericAdmResource;
import org.semanticwb.portal.api.SWBParamRequest;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author sergio.tellez
 */
public class ArtDetail extends GenericAdmResource {

    private static final String IDENTIFIER = "id";
    private static final Logger LOG = Logger.getLogger(ArtDetail.class.getName());

    @Override
    public void doView(HttpServletRequest request, HttpServletResponse response, SWBParamRequest paramRequest) throws IOException {
        Entry entry;
        String uri = getResourceBase().getAttribute("endpointURL","http://localhost:9200") + "/api/v1/search?identifier=";
        String JSPPath = "/swbadmin/jsp/rnc/artdetail.jsp";
        RequestDispatcher rd = request.getRequestDispatcher(JSPPath);

        if (null != request.getParameter(IDENTIFIER)) {
            uri += request.getParameter(IDENTIFIER);
            URL url = new URL(uri);

            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            InputStream is = connection.getInputStream();
            String jsonText = SWBUtils.IO.readInputStream(is, "UTF-8");
            Gson gson = new Gson();

            entry = gson.fromJson(jsonText, Entry.class);

            if (null != entry) {
                request.setAttribute("entry", entry);

                //Update object view count
                uri = getResourceBase().getAttribute("endpointURL","http://localhost:9200")
                        + "/api/v1/search/hits/"
                        + entry.getId();

                url = new URL(uri);
                connection = (HttpURLConnection)url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.getOutputStream().close();
                int code = connection.getResponseCode();
            }
        }

        request.setAttribute("paramRequest", paramRequest);

        try {
            rd.include(request, response);
        } catch (ServletException se) {
            se.printStackTrace();
            LOG.info(se.getMessage());
        }
    }

    private String testJson() {
        Entry entry = new Entry();
        Identifier identifier = new Identifier();
        identifier.setType("Lorem Ipsum Dolor");
        identifier.setValue("45");
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(identifier);
        entry.setIdentifier(identifiers);
        DigitalObject digital = new DigitalObject();
        Rights stone = new Rights();
        stone.setUrl("/work/models/cultura/Resource/45/carrusel-02.jpg");
        List<Rights> rights = new ArrayList<>();
        rights.add(stone);
        Rights calendar = new Rights();
        calendar.setUrl("/work/models/cultura/Resource/46/carrusel-01.jpg");
        rights.add(calendar);
        digital.setRights(rights);
        List<DigitalObject> digitalobject = new ArrayList<>();
        digitalobject.add(digital);
        entry.setDigitalobject(digitalobject);
        Title title = new Title();
        title.setValue("Piedra del Sol");
        List<Title> titles = new ArrayList<>();
        titles.add(title);
        entry.setRecordtitle(titles);
        DateDocument pdatecreated = new DateDocument();
        pdatecreated.setValue("Desconocido, Posclásico tardío (1250-1521 d.C.)");
        Period periodcreated = new Period();
        periodcreated.setDatestart(pdatecreated);
        entry.setPeriodcreated(periodcreated);
        DateDocument datecreated = new DateDocument();
        datecreated.setDatevalue(new Date());
        entry.setDatecreated(datecreated);
        List<String> creator = new ArrayList<>();
        creator.add("Lorem Ipsum Dolor");
        entry.setCreator(creator);

        entry.setCollection("Lorem Ipsum Dolor");
        entry.setInstitution("INBA");
        entry.setDescription("Monumento colosal con la imagen labrada del disco solar representado como una sucesión de anillos concéntricos con diferentes elementos. En su centro se encuentra el glifo ?4 movimiento? (nahui ollin), que rodea el rostro de una deidad solar. El siguiente anillo contiene los 20 signos de los días; alrededor de éste se encuentra otro anillo, labrado con cuadretes que simbolizan los 52 años de un siglo mexica. Dos grandes serpientes de turquesa o xiuhcóatl envuelven todos estos elementos y unen su cabeza en la parte inferior del monolito.");
        entry.setTechnique("Lorem Ipsum Dolor");
        Gson gson = new Gson();
        return gson.toJson(entry);
    }
}
