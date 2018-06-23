<%-- 
    Document   : zoomdetail.jsp
    Created on : 13/12/2017, 10:28:47 AM
    Author     : sergio.tellez
--%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@page import="org.semanticwb.portal.api.SWBResourceURL"%>
<%@page import="org.semanticwb.portal.api.SWBParamRequest"%>
<%@page import="java.util.List, mx.gob.cultura.portal.response.Entry, mx.gob.cultura.portal.response.DigitalObject, mx.gob.cultura.portal.response.Rights"%>
<%
	int index = 0;
	List<DigitalObject> digitalobjects = null;
    SWBParamRequest paramRequest = (SWBParamRequest)request.getAttribute("paramRequest");
    Entry entry = null != request.getAttribute("entry") ? (Entry)request.getAttribute("entry") : new Entry();
	entry.setDescription("El London Eye ('Ojo de Londres'), tambi�n conocido como Millennium Wheel ('Noria del milenio'), es una noria-mirador de 135 m situada sobre el extremo occidental de los Jubilee Gardens, en el South Bank del T�mesis, distrito londinense de Lambeth, entre los puentes de Westminster y Hungerford. La noria est� junto al County Hall y frente a las oficinas del Ministerio de Defensa.");
	
%>
<!-- Plyr core script -->
<script src="https://cdn.plyr.io/2.0.18/plyr.js"></script>
<script>plyr.setup();</script>
<script type="text/javascript" src="/swbadmin/js/dojo/dojo/dojo.js" djConfig="parseOnLoad: true, isDebug: false, locale: 'en'"></script>

<script type="text/javascript" src="/work/models/repositorio/js/viewer-video.js"></script>
<link rel='stylesheet' type='text/css' media='screen' href='/work/models/repositorio/css/viewer-video.css'/>

<script>
	function add(id) {
		dojo.xhrPost({
            url: '/swb/cultura/favorito?id='+id,
            load: function(data) {
                dojo.byId('addCollection').innerHTML=data;
				$('#addCollection').modal('show');
            }
        });
	}
	function addPop(id) {
		var leftPosition = (screen.width) ? (screen.width-990)/3 : 0;
		var topPosition = (screen.height) ? (screen.height-150)/3 : 0;
		var url = '/swb/cultura/favorito?id='+id;
		popCln = window.open(
		url,'popCln','height=220,width=990,left='+leftPosition+',top='+topPosition+',resizable=no,scrollbars=no,toolbar=no,menubar=no,location=no,directories=no,status=no')
	}
	function loadDoc(id) {
		var xhttp = new XMLHttpRequest();
		xhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			jQuery("#addCollection-tree").html(this.responseText);
			$("#addCollection" ).dialog( "open" );
		}else if (this.readyState == 4 && this.status == 403) {
			jQuery("#dialog-message-tree").text("Reg�strate o inicia sesi�n para crear tus colecciones.");
			$("#dialog-message-tree" ).dialog( "open" );
		}
	  };
	  xhttp.open("GET", "/swb/cultura/favorito?id="+id, true);
	  xhttp.send();
	}
	function loadImg(iEntry, iDigit) {
		dojo.xhrPost({
			url: '/es/cultura/detalle/_rid/64/_mto/3/_mod/DIGITAL?id='+iEntry+'&n='+iDigit,
            load: function(data) {
                dojo.byId('idetail').innerHTML=data;
            }
        });
	}
</script>

<script>
$(document).ready(function(){
    $("#play-pause").click(function(){
        $(".obranombre").toggleClass("opaco");
    });
});
</script>
<style type="text/css">
    #detalle { height:100% !important}
    #bodyDetalle section.contenido { height:100%}
    .obranombre { bottom:60px !important; top:auto;}
</style>
  

<section id="detalle">
	<div id="idetail" class="detalleimg">

		 <div class="obranombre">
             <h3 class="oswB">T�tulo de Video</h3>
             <p class="oswL">Nombre del Autor</p>
         </div>
		
		 <video id="video" poster="/work/models/repositorio/img/video.jpg" controls controlsList="nodownload">
			<source src="https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-HD.mp4" type="video/mp4">
			<p>Tu navegador no soporta video en HTML5</p>
		</video>
		<div id="video-controls">
			<button type="button" id="play-pause" class="play"><span class="ion-ios-play"></span></button>
			<input type="range" id="seek-bar" value="0">
			<button type="button" id="mute"><span class="ion-ios-volume-high"></span></button>
			<input type="range" id="volume-bar" min="0" max="1" step="0.1" value="1">
			<button type="button" id="full-screen"><span class="ion-android-expand"></span></button>
		</div>
		
	</div>
</section>