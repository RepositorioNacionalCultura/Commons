<%-- 
    Document   : artdetail.jsp
    Created on : 5/12/2017, 11:48:36 AM
    Author     : sergio.tellez
--%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@page import="mx.gob.cultura.portal.response.DigitalObject"%>
<%@page import="mx.gob.cultura.portal.response.Entry"%>
<%@page import="org.semanticwb.portal.api.SWBParamRequest, java.util.Iterator, java.util.List"%>
<%
	int index = 0;
    SWBParamRequest paramRequest = (SWBParamRequest)request.getAttribute("paramRequest");
    Entry entry = (Entry)request.getAttribute("entry");
	List<DigitalObject> digitalobjects = entry.getDigitalobject();
%>
<main role="main" class="container-fluid detalle" id="detalle">
    <div class="row row-offcanvas row-offcanvas-right" id="detallecont">
        <div class="col-12 col-md-9" id="contenido">
            <p class="float-right d-md-none">
                <button type="button" class="btn btn-sm" data-toggle="offcanvas">
                    <span class="ion-chevron-left"> Mostrar ficha </span>
                    <span class="ion-chevron-right"> Ocultar ficha </span>
                </button>
            </p>
            <div id="carouselExampleIndicators" class="carousel slide" data-ride="carousel">
                <ol class="carousel-indicators">
					<% 
								int i = 0;
						for (DigitalObject r : digitalobjects) {
							if (r.getMediatype().getMime().equals("image/jpeg")) {
									if (i == 0) {
							%>
										<li data-target="#carouselExampleIndicators" data-slide-to="0" class="active"></li>
							<%		
									}else {
							%>
										<li data-target="#carouselExampleIndicators" data-slide-to="<% out.println(""+i); %>"></li>
							<% 
									}
									i++;
							 }
					}
					%>
                </ol>
                <div class="carousel-inner">
					<% 
						for (DigitalObject r : digitalobjects) {
							if (r.getMediatype().getMime().equals("image/jpeg")) {
										if (index == 0) {
								%>
											<div class="carousel-item active">
								<%		
										}else {
								%>	
											<div class="carousel-item">
								<%	
										} 
								%>
												<img src="<%=r.getUrl()%>" style="max-width: 581px; max-height: 476px;" alt="siguiente" class="img-responsive"></img>
											</div>
								<% 
										index++;
							}
						} 
					%>    
                </div>
				<%
					if (index > 1) {
				%>
						<a class="carousel-control-prev" href="#carouselExampleIndicators" role="button" data-slide="prev">
							<i class="fa fa-long-arrow-left" aria-hidden="true"></i>
							<span class="sr-only">anterior</span>
						</a>
						<a class="carousel-control-next" href="#carouselExampleIndicators" role="button" data-slide="next">
							<i class="fa fa-long-arrow-right" aria-hidden="true"></i>
							<span class="sr-only">siguiente</span>
						</a>
				<%
					}
				%>
            </div>
        </div>
        <div class="col-6 col-md-3 sidebar-offcanvas" id="sidebar">
            <div>
                <p>Ficha t�cnica</p>
                <hr>
                <ul>
					<%
						String type = "";
						Iterator<String> it = entry.getResourcetype().iterator();
						while (it.hasNext()) {
						    String t = it.next();
						    type += t;
						    if (it.hasNext()) {
						        type += ", ";
							}
						}

						String createdDate = "";
						if (null != entry.getDatecreated()) {
						    createdDate = entry.getDatecreated().getValue();
                        } else if (null != entry.getPeriodcreated()) {
                            createdDate = entry.getPeriodcreated().getName();
                        }

                        String creator = "";
						if (null != entry.getCreator() && !entry.getCreator().isEmpty()) {
						    creator = entry.getCreator().get(0);
                        }
					%>
                    <li><strong>Tipo de objeto</strong> <%=type%></li>
                    <li><strong>Autor</strong> <%=creator%></li>
                    <li><strong>T�tulo</strong> <%=entry.getRecordtitle().get(0).getValue()%></li>
                    <li><strong>Fecha de creaci�n</strong> <%=createdDate%></li>
                    <!--li><strong>T�cnica</strong> <%=entry.getTechnique()%></li-->
                    <li><strong>Instituci�n</strong> <%=entry.getHolder()%></li>
                    <!--li><strong>Fondo o colecci�n</strong> <%=entry.getCollection()%></li-->
                    <li><strong>Identificador</strong> <%=entry.getIdentifier().get(0).getValue()%></li>
                </ul>
            </div>
            <div class="vermas">
                <a href="#">Ver m�s <span class="ion-android-add-circle"></span></a>
            </div>
        </div>
    </div>
    <div id="detallesube" class="row detalle-acciones ">
        <div class="col-xs-12 col-sm-7 col-md-6 col-lg-6 col-xl-6 offset-sm-0 offset-md-0 offset-lg-0 offset-xl-1">
			<a href="#detallesube" id="subir"><i class="fa fa-long-arrow-down rojo-bg" aria-hidden="true"></i></a>
            <p class="oswB"><%=entry.getRecordtitle().get(0).getValue()%></p>
            <p><%=createdDate%></p>
        </div>
        <div class="col-xs-12 col-sm-2 col-md-3 col-lg-3 col-xl-2">
            <a href="#"><i class="fa fa-search-plus" aria-hidden="true"></i></a>
            <a href="#"><i class="fa fa-heart" aria-hidden="true"></i></a>
            <a href="#"><i class="fa fa-share-alt" aria-hidden="true"></i></a>
            <a href="#"><i class="fa fa-download" aria-hidden="true"></i></a>
        </div>
        <div class="col-xs-12 col-sm-2 col-md-3 col-lg-3 col-xl-3 rojo">
            <p>�Puedo usarlo?</p>
        </div>
    </div>
</main>
<section id="detalleinfo">
	<a></a>
    <div class="cointainer-fluid">
		<div class="row detalleinfo azul-bg">
			<p class="col-md-10 offset-md-1">
				<%=entry.getDescription()%>
			</p>
		</div>
	</div>
</section>