<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, org.semanticwb.model.WebSite, org.semanticwb.portal.api.SWBParamRequest" %>
<%@ page import="mx.gob.cultura.portal.response.Title, mx.gob.cultura.portal.response.Collection, mx.gob.cultura.portal.response.DigitalObject, mx.gob.cultura.portal.response.Entry, mx.gob.cultura.portal.response.Identifier"%>
<%
    Collection c = (Collection) request.getAttribute("collection");
    List<Entry> itemsList = (List<Entry>) request.getAttribute("myelements");
    SWBParamRequest paramRequest = (SWBParamRequest) request.getAttribute("paramRequest");
    WebSite site = paramRequest.getWebPage().getWebSite();
%>

<div class="container coleccionSecc">
    <div class="row">
        <div class="col-9 col-sm-8 coleccionSecc-01">
            <div class="precontent">
                <h2 class="oswM rojo">Lorem ipsum dolor sit amet</h2>
                <div class="row perfilHead">
                    <img src="/work/models/repositorio/img/agregado-07.jpg" class="circle">
                    <p>Leonor Rivas Mercado, 10 de marzo de 2018</p>
                </div>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor inetum incididunt ut labore et dolore magna aliqua minim veniam, quis nostrud.</p>
                <hr class="rojo">
                <div class="row redes">
                    <a href="#"><span class="ion-social-facebook"></span> Compartir</a>
                    <a href="#"><span class="ion-social-twitter"></span> Tweet</a>
                    <a href="#" class="rojo"><span class="ion-heart rojo"></span> Favoritos (356)</a>
                </div>
            </div>
        </div>
        <div class="col-3 col-sm-4 coleccionSecc-02">
            <button type="submit" class="btn-cultura btn-blanco btn-mayus d-none d-md-block"><span class="ion-edit"></span> Editar colección</button>
            <button type="submit" class="btn-cultura btn-blanco btn-mayus d-block d-md-none"> <span class="ion-edit"></span> Editar</button>
        </div>
    </div>
</div>
<div class="container" style="padding:30px; text-align: center; background:#efefef">
    <div class="row offcanvascont">
        <div id="contenido">
            <%
                if (!itemsList.isEmpty()) {
            %>

            <div id="references">
                <div id="resultados" class="card-columns">
                    <%
                        for (Entry item : itemsList) {
                            Title title = new Title();
                            DigitalObject digital = new DigitalObject();
                            List<String> creators = item.getCreator();
                            List<Title> titles = item.getRecordtitle();
                            List<String> resourcetype = item.getResourcetype();
                            List<DigitalObject> digitalobject = item.getDigitalobject();
                            if (!titles.isEmpty()) {
                                title = titles.get(0);
                            }
                            String creator = creators.size() > 0 ? creators.get(0) : "";
                            String type = resourcetype.size() > 0 ? resourcetype.get(0) : "";
                            if (!digitalobject.isEmpty()) {
                                digital = digitalobject.get(0);
                            }
                    %>
                    <div class="pieza-res card">
                        <a href="/swb/<%=site.getId()%>/detalle?id=<%=item.getId()%>">
                            <img src="<%=digital.getUrl()%>" />
                        </a>
                        <div>
                            <p class="oswB azul tit"><a href="#"><%=title.getValue()%></a></p>
                            <p class="azul autor"><a href="#"><%=creator%></a></p>
                            <p class="tipo"><%=type%></p>
                        </div>
                    </div>
                    <%
                        }
                    %>
                </div>
            </div>
            <%
                } else
                    out.print("<h3 class=\"oswB azul\">Agregue favoritos a su colección " + c.getTitle() + "</h3>");
            %>
        </div>
    </div>
    <!--resultados -->
</div>
<div class="coleccionSecc-03 col-12 col-md-8 col-lg-6">
    <div class="agregarColecc ">
        <a href="#">
            <span class="ion-ios-plus"></span>
            <em class="oswM">Agregar  desde la colección</em>
            <span class="btn-cultura">Explorar <span class="ion-chevron-right"></span></span>
        </a>
        <div>
            <img src="/work/models/repositorio/img/cabecera-carranza.jpg">
        </div>
    </div>
</div>