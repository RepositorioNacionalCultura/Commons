<%-- 
    Document   : audiodetail.jsp
    Created on : 22/05/2018, 11:48:36 AM
    Author     : sergio.tellez
--%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ page import="mx.gob.cultura.portal.utils.Utils, org.semanticwb.model.WebSite, org.semanticwb.portal.api.SWBResourceURL, org.semanticwb.portal.api.SWBParamRequest"%>
<%@ page import="java.util.List, java.util.ArrayList, mx.gob.cultura.portal.response.Title, mx.gob.cultura.portal.response.Entry, mx.gob.cultura.portal.response.DateDocument, mx.gob.cultura.portal.response.DigitalObject"%>
<script type="text/javascript" src="/swbadmin/js/rnc/detail.js"></script>
<script type="text/javascript" src="/swbadmin/js/dojo/dojo/dojo.js" djConfig="parseOnLoad: true, isDebug: false, locale: 'en'"></script>
<%
    int audios = 0;
    String title = "";
    //String period = "";
    String creator = "";
    //String resource = "";
    List<Title> titles = new ArrayList<>();
    List<String> creators = new ArrayList<>();
    StringBuilder divVisor = new StringBuilder();
    StringBuilder scriptHeader = new StringBuilder();
    StringBuilder scriptCallVisor = new StringBuilder();
    List<DigitalObject> digitalobjects = new ArrayList<>();
    Entry entry = (Entry)request.getAttribute("entry");
    SWBParamRequest paramRequest = (SWBParamRequest)request.getAttribute("paramRequest");
    WebSite site = paramRequest.getWebPage().getWebSite();
    if (null != entry) {
	if (null != entry.getDigitalObject()) {
            creators = entry.getCreator();
            titles = entry.getRecordtitle();
            digitalobjects = entry.getDigitalObject();
            creator = creators.size() > 0 ? creators.get(0) : "";
            audios = null != digitalobjects ? digitalobjects.size() : 0;
            if (!titles.isEmpty()) title = titles.get(0).getValue();
            if (audios > 0) {
                scriptHeader.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"/work/models/").append(site.getId()).append("/audio/css/audio.css\"/>");
		divVisor.append("<script>")
                    .append("	$(document).ready(function(){")
                    .append("       $(\"#play-pause\").click(function(){")
                    .append("		$(\".audio-bit\").toggleClass(\"playbit\");")
                    .append("       });")
                    .append("	});")
                    .append("</script>");
                scriptCallVisor.append("<script type=\"text/javascript\">")
                    .append("   Amplitude.init({")
                    .append("       \"songs\": [");
                    for (DigitalObject digital : digitalobjects) {
                        String type = (null != digital.getMediatype() && null != digital.getMediatype().getMime()) ?  digital.getMediatype().getMime() : "";
			if (!type.isEmpty() && type.startsWith("audio")) {
                            scriptCallVisor.append("    {")
                                .append("	\"name\": \"").append(title).append("\",")
				.append("	\"artist\": \"").append(creator).append("\",")
				.append("	\"album\": \"").append(null != digital.getMediatype() ? digital.getMediatype().getName() : "").append("\",")
				.append("	\"url\": \"").append(digital.getUrl()).append("\",")
				.append("	\"cover_art_url\": \"/work/models/").append(site.getId()).append("/audio/img/waves.png\"")
				.append("   },");
			}
                    }
                    scriptCallVisor.append("       ]")
			.append("	});")
			.append("</script>");
                    //resource = entry.getResourcetype().size() > 0 ? entry.getResourcetype().get(0) : "";
                    //period = null != entry.getDatecreated() ? Utils.esDate(entry.getDatecreated().getValue()) : "";
            }
        }
    }
    String back = (String)request.getAttribute("back");
    String scriptFB = Utils.getScriptFBShare(request);
%>
<%=scriptFB%>
<%=scriptHeader%>
<%=divVisor%>
<section id="detalle">
    <div id="idetail" class="detallelist">
        <div class="explora">
            <div class="explora2">
                <div class="explo1">
                    � <%=paramRequest.getLocaleString("usrmsg_view_detail_all_rights")%>
                </div>
		<div class="explo2 row">
                    <div class="col-3">
                        <<a href="#" onclick="fbShare();"><span class="ion-social-facebook"></span></a>
                    </div>
                    <div class="col-3">
                        <span class="ion-social-twitter"></span>
                    </div>
                    <div class="col-6">
                        <a href="#" onclick="loadDoc('/swb/<%=site.getId()%>/favorito?id=', '<%=entry.getId()%>');"><span class="ion-heart"></span></a> <%=entry.getResourcestats().getViews()%>
                    </div>
                </div>
                <div class="explo3 row">
                    <div class="col-6">
                    </div>
                    <div class="col-6">
                    </div>
                </div>
            </div>
        </div>
        <div class="" id="amplitude-player">
            <div class="row audiorow">
                <div class="col-12 col-sm-8" id="amplitude-left">
                    <div id="meta-container">
                        <div class="row">
                            <div class="bit1 col-3 col-md-4">
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit-100"></div>
                            </div>
                            <div class="bit2 col-9 col-md-8">
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>                                    
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit"></div>
                                <div class="audio-bit-100"></div>
                            </div>
                            <div class="bit3 col-3 col-md-4">
                                <div class="audio-bit2"></div>
                                <div class="audio-bit-100"></div>
                            </div>
                            <div class="bit4 col-9 col-md-8">
                                <span amplitude-song-info="name" amplitude-main-song-info="true" class="song-name oswM"></span>
                                <div class="song-artist-album">
                                    <span amplitude-song-info="album" amplitude-main-song-info="true" class="oswM"></span>
                                    <span amplitude-song-info="artist" amplitude-main-song-info="true"></span>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div id="player-left-bottom" class="barra-controles row">
                        <div class="col-12 col-md-8">
                            <div id="control-container" class="col-12">
                                <div id="central-control-container"> 
                                    <div id="central-controls">
                                        <div class="amplitude-prev" id="previous">
                                            <span class="ion-ios-skipbackward"></span>
                                        </div>
                                        <div class="amplitude-play-pause" amplitude-main-play-pause="true" id="play-pause">
                                            <span class="ion-ios-play"></span>
                                            <span class="ion-ios-pause"></span>
                                        </div>
                                        <div class="amplitude-next" id="next">
                                            <span class="ion-ios-skipforward"></span>
                                        </div>
                                    </div>
                                </div> <!-- pre PLAY next -->
                            </div> <!-- CONTROLES -->
                            <div id="time-container" class="col-12"> 
                                <span class="current-time"> 
                                    <span class="amplitude-current-minutes oswM" amplitude-main-current-minutes="true"></span><b>:</b><span class="amplitude-current-seconds oswM" amplitude-main-current-seconds="true"></span>
                                </span> <!-- 00 : 00-->
                                <input type="range" class="amplitude-song-slider" amplitude-main-song-slider="true" step=".1"/> <!-- linea --->
                                <span class="duration"> 
                                    <span class="amplitude-duration-minutes oswM" amplitude-main-duration-minutes="true"></span><b>:</b><span class="amplitude-duration-seconds oswM" amplitude-main-duration-seconds="true"></span>
                                </span> <!-- 33 : 33-->
                            </div> <!-- TIEMPO -->
                        </div>
                        <div id="volume-container" class="col-4">
                            <div class="volume-controls row">
                                <div class="amplitude-mute col-3 col-sm-3 col-md-2 amplitude-not-muted">
                                    <span class="ion-ios-volume-high"></span>
                                    <span class="ion-ios-volume-low"></span>
                                </div>
                                <div class="col-8 col-sm-9 col-md-10">
                                    <input type="range" class="amplitude-volume-slider" />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
		<div class="col-12 col-sm-4" id="amplitude-right">
                <%
                    int index = 0;
                    for (DigitalObject digital : digitalobjects) {
                        int id = index + 1;
                        String type = (null != digital.getMediatype() && null != digital.getMediatype().getMime()) ?  digital.getMediatype().getMime() : "";
                        String song = (null != digital.getMediatype() && null != digital.getMediatype().getName()) ?  digital.getMediatype().getName() : "";
			if (!type.isEmpty() && type.startsWith("audio")) {
		%>
                            <div class="song amplitude-song-container amplitude-play-pause" amplitude-song-index="<%=index%>">
                                <span class="song-number"><%=id%>.</span>
                                <div class="song-meta-data">
                                    <span class="song-title"><%=song%></span>
                                    <span class="song-artist"><%=creator%></span>
                                </div>
                                <span class="song-duration">3:30</span>
                            </div>
                <%
                            index++;
			}
                    } 
                %>
		</div>
            </div>
	</div>
    </div>
</section>
<section id="detalleinfo">
    <div class="container">
	<div class="row">              
            <jsp:include page="../rack.jsp" flush="true"/>
            <jsp:include page="../techdata.jsp" flush="true"/>
            <div class="col-12 col-sm-6  col-md-3 col-lg-3 order-md-3 order-sm-3 order-3 clave">
		<div class="redes">
                    <a href="#" onclick="fbShare();"><span class="ion-social-facebook"></span></a>
                    <span class="ion-social-twitter"></span>
                </div>
                <div>
                    <p class="tit2"><%=paramRequest.getLocaleString("usrmsg_view_detail_key_words")%></p>
                    <p>
                        <% 
                            int i = 0;
                            for (String key :  entry.getKeywords()) {
                                i++;
                                out.println("<a href=\"#\">"+key+"</a>");
                                if (i < entry.getKeywords().size()) out.println(" / ");
                            }
                        %>
                    </p>
                </div>
                <div class="">
                    <a href="<%=back%>">
                        <i aria-hidden="true" class="fa fa-long-arrow-left"></i> <%=paramRequest.getLocaleString("usrmsg_view_detail_back")%>
                    </a>
                </div>
            </div>
        </div>
    </div>
</section>
<%=scriptCallVisor%>
<div id="dialog-message-tree" title="error">
    <p>
        <div id="dialog-text-tree"></div>
    </p>
</div>

<div id="dialog-success-tree" title="�xito">
    <p>
        <span class="ui-icon ui-icon-circle-check" style="float:left; margin:0 7px 50px 0;"></span>
        <div id="dialog-msg-tree"></div>
    </p>
</div>

<div id="addCollection">
    <p>
        <div id="addCollection-tree"></div>
    </p>
</div>

<div class="modal fade" id="newCollection" tabindex="-1" role="dialog" aria-labelledby="modalTitle" aria-hidden="true"></div>