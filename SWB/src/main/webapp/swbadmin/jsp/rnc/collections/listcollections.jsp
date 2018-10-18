<%-- 
    Document   : listcollections
    Created on : 12-oct-2018, 17:37:52
    Author     : juan.fernandez
--%>
<%-- 
    Document   : mycollections
    Created on : 24/01/2018, 05:36:23 PM
    Author     : sergio.tellez
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.semanticwb.portal.api.SWBParamRequest, org.semanticwb.portal.api.SWBResourceURL, mx.gob.cultura.portal.resources.MyCollections, org.semanticwb.model.WebSite, mx.gob.cultura.portal.response.Collection, java.util.List"%>
<%
    List<Collection> boards = (List<Collection>) request.getAttribute("PAGE_LIST");
    SWBParamRequest paramRequest = (SWBParamRequest) request.getAttribute("paramRequest");
    //Use in dialog
    SWBResourceURL saveURL = paramRequest.getActionUrl();
    saveURL.setMode(SWBResourceURL.Mode_VIEW);
    saveURL.setAction(MyCollections.ACTION_ADD);

    SWBResourceURL uedt = paramRequest.getRenderUrl().setMode(SWBResourceURL.Mode_EDIT);
    uedt.setCallMethod(SWBParamRequest.Call_DIRECT);

    SWBResourceURL pageURL = paramRequest.getRenderUrl();
    pageURL.setCallMethod(SWBParamRequest.Call_DIRECT);
    pageURL.setAction("PAGE");

    SWBResourceURL uper = paramRequest.getActionUrl();
    SWBResourceURL udel = paramRequest.getActionUrl();
    uper.setAction(MyCollections.ACTION_STA);
    uper.setCallMethod(SWBParamRequest.Call_DIRECT);
    udel.setAction(SWBResourceURL.Action_REMOVE);

    SWBResourceURL uels = paramRequest.getRenderUrl().setMode(MyCollections.MODE_VIEW_USR);
    uels.setCallMethod(SWBParamRequest.Call_CONTENT);

    SWBResourceURL wall = paramRequest.getRenderUrl().setMode(MyCollections.MODE_VIEW_MYALL);
    wall.setCallMethod(SWBParamRequest.Call_CONTENT);
    
    WebSite site = paramRequest.getWebPage().getWebSite();
    String userLang = paramRequest.getUser().getLanguage();

    Integer allc = (Integer)request.getAttribute("COUNT_BY_STAT");

    Integer cusr = (Integer)request.getAttribute("COUNT_BY_USER");

    SWBResourceURL uall = paramRequest.getRenderUrl().setMode(MyCollections.MODE_VIEW_ALL);
    uall.setCallMethod(SWBParamRequest.Call_CONTENT);
    
%>

<!--<script type="text/javascript" src="/swbadmin/js/dojo/dojo/dojo.js" djConfig="parseOnLoad: true, isDebug: false, locale: 'en'"></script>-->
<!--<script>
	dojo.require("dijit.dijit"); // loads the optimized dijit layer
	dojo.require('dijit.Dialog');
</script>-->
<script type="text/javascript">
    
    function doPage(p) {
        dojo.xhrPost({
            url: '<%=pageURL%>?p=' + p,
            load: function (data) {
                dojo.byId('references').innerHTML = data;
                location.href = '#showPage';
            }
        });
    }
</script>

<div class="container usrTit">
    <div class="row">
        <!--<img src="/work/models/<%=site.getId()%>/img/agregado-07.jpg" class="circle">-->
        <div>
            <h2 class="oswM nombre"><%=paramRequest.getUser().getFullName()%></h2>
<!--            <p class="subnombre">Lorem ipsum dolor sit amet, consecetur adipscing elit.</p>
            <button class="btn-cultura btn-blanco" onclick="javascript:location.replace('/<%=userLang%>/<%=site.getId()%>/Registro');">EDITAR PERFIL</button>-->
        </div>
    </div>
<!--    <div class="buscacol">
        <button class="" type="submit"><span class="ion-search"></span></button>
        <input id="buscaColeccion" class="form-control" type="text" placeholder="BUSCA EN LAS COLECCIONES... " aria-label="Search">
    </div>-->
</div>
<!--<div class="menuColecciones">
    <a href="<%//=wall%>" class="selected">Mis colecciones (<%//=cusr%>)</a>
    <a href="#" class="">Mis favoritos (0)</a>
    <a href="#" class="">Recomendados (20)</a>
    <a href="<%//=uall%>" class="">Todos (<%//=allc%>)</a>
    <a href="#" class="">Temas (1)</a>
</div>-->
<a name="showPage"></a>
<div class="container">
    <div id="references">
        <div class="row mosaico-contenedor">
        <%
           //if (MyCollections.MODE_VIEW_MYALL.equalsIgnoreCase(paramRequest.getMode())) {
	%>
<!--                <div class="col-6 col-md-4">
                    <div class="mosaico radius-overflow">
                        <a href="#" data-toggle="modal" data-target="#modalExh">
                            <span class="ion-ios-plus rojo"></span>
                        </a>
                    </div>
                    <div class="mosaico-txt ">
                        <p><span class="ion-locked rojo"></span> Crear colección</p>
                        <p>Lorem ipsum dolor sit</p>
                    </div>
                </div>-->
            <%
                //}
                if (!boards.isEmpty()) {
                    for (Collection c : boards) {
            %>
                        <div class="col-6 col-md-4">
                            <%	if (c.getCovers().isEmpty()) {	%>
                                    <div class="mosaico mosaico1 radius-overflow">
					<a href="<%=uels%>?id=<%=c.getId()%>">
                                            <img src="/work/models/<%=site.getId()%>/img/empty.jpg">
					</a>
                                    </div>
                            <%  }else if (c.getCovers().size() < 3) { %>
                                    <div class="mosaico mosaico1 radius-overflow">
					<a href="<%=uels%>?id=<%=c.getId()%>">
                                            <img src="<%=c.getCovers().get(0)%>">
					</a>
                                    </div>
                            <%  }else { %>
                                    <div class="mosaico mosaico3 radius-overflow">
					<a href="<%=uels%>?id=<%=c.getId()%>">
                                            <div class="mosaico3a">
						<img src="<%=c.getCovers().get(0)%>">
                                            </div>
                                            <div class="mosaico3b">
                                                <div>
                                                    <img src="<%=c.getCovers().get(1)%>">
						</div>
                                                <div>
                                                    <img src="<%=c.getCovers().get(2)%>">
						</div>
                                            </div>
                                        </a>
                                    </div>
                            <% } %>
                            <div class="mosaico-txt ">
                                <p><%=c.getTitle()%></p>
<!--                                <p>Curada por: <%//=paramRequest.getUser().getFullName()%></p>
                                <a href="#"><span class="ion-social-facebook"></span></a>
                                <a href="#"><span class="ion-social-twitter"></span></a>
                                <a href="#" onclick="messageConfirm('¿Está usted seguro de eliminar la colección?', '<%=c.getId()%>');"><span class="ion-trash-a"></span></a>
                                <a href="#" onclick="editByForm('<%//=c.getId()%>');"><span class="ion-edit"></span></a>-->
                            </div>
                        </div>
            <%
                    }
                }
            %>
        </div>
    </div>
    <jsp:include page="pager.jsp" flush="true"/>
</div>
<!--<div class="coleccionSecc-03 col-12 col-md-8 col-lg-6">
    <div class="agregarColecc ">
        <a href="#" onclick="javascript:location.replace('/<%//=userLang%>/<%//=site.getId()%>/coleccion');">
            <span class="ion-ios-plus"></span>
            <em class="oswM">Agregar  desde la colección</em>
            <span class="btn-cultura">Explorar <span class="ion-chevron-right"></span></span>
        </a>
        <div>
            <img src="/work/models/repositorio/img/cabecera-carranza.jpg">
        </div>
    </div>
</div>-->
<!-- MODAL -->
<div class="modal fade" id="modalExh" tabindex="-1" role="dialog" aria-labelledby="modalTitle" aria-hidden="true">
    <div class="modal-dialog modal-exh modal-2col" role="document">
        <div class="modal-content">
            <div class="row">
                <div class="col-4 col-sm-5 modal-col1">
                    <div class="modal-izq">
                        <img src="/work/models/repositorio/img/cabecera-colaborar.jpg">    
                    </div>
                </div>
                <div class="col-8 col-sm-7 modal-col2">
                    <div class="modal-header">
                        <h4 class="modal-title oswM rojo">CREAR NUEVA COLECCIÓN</h4>
                        <button type="button" class="close" data-dismiss="modal">
                            <span class="ion-ios-close-outline"></span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <form id="addCollForm" action="<%=saveURL.toString()%>" method="post">
                            <div class="form-group">
                                <label for="crearNombre">Nombre</label>
                                <input type="text" name="title" maxlength="100" value="" id="crearNombre" class="form-control" placeholder="60" aria-label="Recipient's username" aria-describedby="basic-addon2"/>
                                <label for="crearDescr">Descripción (opcional)</label>
                                <textarea name="description" id="crearDescr" placeholder="250"></textarea>        
                                <label for="selprivado" class="selPrivado">
                                    <input name="status" value="" id="selprivado" type="checkbox" aria-label="Checkbox for following text input"/>
                                    <span class="ion-locked"> Privado</span>
                                </label>
                            </div>
                            <button type="button" onclick="save();" class="btn-cultura btn-rojo btn-mayus">Crear colección</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="addCollection" role="dialog">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title">Agregar colección</h4>
                <button type="button" class="close" data-dismiss="modal">&times;</button>
            </div>
            <div class="modal-body">
                <p>
                    <form id="addCollForm" action="<%=saveURL.toString()%>" method="post">
                        <div class="col-xs-12 col-sm-12 col-md-10 col-lg-10 car-img2">
                            <div class="card-body">
                                <span class="card-title">* Nombre: </span><input type="text" name="title" maxlength="100" size="40" value=""/><div id="dialog-msg"></div>
                            </div>
                            <div class="card-body">
                                <span class="card-title">Descripción: </span><textarea name="description" rows="4" cols="40" maxlength="500" wrap="hard"></textarea>
                            </div>
                            <div class="card-body">
                                <span class="card-title">Público: </span><input type="checkbox" name="status" value=""/>
                            </div>
                        </div>
                    </form>
                </p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-sm rojo" data-dismiss="modal">Cerrar</button>
                <button type="button" onclick="save();" class="btn btn-sm rojo">Guardar</button>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="alertSuccess" role="dialog">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title">Éxito</h4>
                <button type="button" class="close" data-dismiss="modal">&times;</button>
            </div>
            <div class="modal-body">
                <div id="dialog-text"></div>
            </div>
            <div class="modal-footer">
                <button type="button" id="closeAlert" class="btn btn-sm rojo" data-dismiss="modal">Cerrar</button>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="editCollection" tabindex="-1" role="dialog" aria-labelledby="modalTitle" aria-hidden="true"></div>