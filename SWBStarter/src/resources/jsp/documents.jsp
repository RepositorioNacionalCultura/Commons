<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@page import="org.semanticwb.portal.api.SWBResourceURL"%>
<%@page import="org.semanticwb.portal.api.SWBParamRequest"%>
<%@page import="java.util.List,mx.gob.cultura.portal.response.Entry, mx.gob.cultura.portal.response.Identifier, mx.gob.cultura.portal.response.DigitalObject, mx.gob.cultura.portal.response.Title, mx.gob.cultura.portal.response.Aggregation"%>
<script type="text/javascript" src="/swbadmin/js/dojo/dojo/dojo.js" djConfig="parseOnLoad: true, isDebug: false, locale: 'en'" ></script>
<%
    SWBParamRequest paramRequest = (SWBParamRequest)request.getAttribute("paramRequest");
    SWBResourceURL pageURL = paramRequest.getRenderUrl().setMode("PAGE");
    pageURL.setCallMethod(SWBParamRequest.Call_DIRECT);
    SWBResourceURL pagesURL = paramRequest.getRenderUrl().setMode("PAGES");
    pagesURL.setCallMethod(SWBParamRequest.Call_DIRECT);
    List<Entry> references = (List<Entry>)session.getAttribute("PAGE_LIST");
    String word = (String)request.getAttribute("word");
%>
<script type="text/javascript">
	function setList() {
		doPage(1, 'l');
	}
	function setGrid() {
		doPage(1, 'g');
	}
    function doPage(p, m) {
        dojo.xhrPost({
            url: '<%=pageURL%>?p='+p+'&m='+m,
            load: function(data) {
				dojo.byId('references').innerHTML=data; 
            }
        });
    }
</script>
<jsp:include page="filters.jsp" flush="true"/>
<% if (!references.isEmpty()) {  %>
    <div id="references">
        <div id="recientes" class="row">
    <%      
			for (Entry reference : references) {
				String creator = "";
				Title title = new Title();
				Identifier identifier = new Identifier();
				DigitalObject digital = new DigitalObject();
				List<Title> titles = reference.getRecordtitle();
				List<String> creators = reference.getCreator();
				List<DigitalObject> digitalobject = reference.getDigitalobject();
				List<Identifier> identifiers = reference.getIdentifier();
				if (!digitalobject.isEmpty()) digital = digitalobject.get(0);
				if (!titles.isEmpty()) title = titles.get(0);
				if (!creators.isEmpty()) creator = creators.get(0);
				for (Identifier id : identifiers) {
					if (id.isPreferred()) identifier = id;
				}
	%>	
				<div class="pieza">
					<div>
						<a href="/swb/cultura/detalle?id=<%=identifier.getValue()%>">
							<img src="<%=digital.getUrl()%>" />
						</a>
					</div>
					<p class="oswB azul tit"><a href="#"><%=title.getValue()%></a></p>
					<p class="azul autor"><a href="#"><%=creator%></a></p>
				</div>
		<%      
			}
		%>
        </div>
        <jsp:include page="pager.jsp" flush="true"/>
    </div>
<%
    }else if (null != word) { out.println("No se encontraron resultados para la b�squeda " + word); }
        else { out.println("Debe proporcionar un criterio de b�squeda"); }
%>