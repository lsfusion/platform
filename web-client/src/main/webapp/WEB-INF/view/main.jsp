<%@ page import="lsfusion.base.ServerMessages" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    	<meta name="viewport" content="width=device-width, initial-scale=1">

        <title>${title}</title>
        <link rel="shortcut icon" href="${logicsIcon}" />
        <link id="themeCss" rel="stylesheet" type="text/css" href="static/css/light.css"/>

        <style>
            @import url('static/css/fontsGoogle/fonts_googleapis_OpenSans.css');
        </style>
        
        <style type="text/css">
            #loading {
                border: 1px solid #ccc;
                position: absolute;
                left: 45%;
                top: 40%;
                padding: 2px;
                z-index: 20001;
                height: auto;
            }

            #loading a {
                color: #225588;
            }

            #loading .loadingIndicator {
                background: white;
                font: bold 13px tahoma, arial, helvetica;
                padding: 10px;
                margin: 0;
                height: auto;
                color: #444;
            }

            #loadingGif {
                vertical-align:top;
            }

            #loadingMsg {
                font: normal 13px arial, tahoma, sans-serif;
            }
        </style>

        <script type="text/javascript" src="static/noauth/js/loadResources.js"></script>
        <script>
            loadResources([

                <!-- need jquery for pivot table -->
                <!-- version jquery above 2.2.4 causes to errors in the pivot table -->
                'https://cdnjs.cloudflare.com/ajax/libs/jquery/2.2.4/jquery.min.js',
                'https://cdnjs.cloudflare.com/ajax/libs/jqueryui/1.12.1/jquery-ui.min.js',

                <!-- export pivot to excel -->
                'static/js/tableToExcel.js',

                <!-- optional: mobile support with jqueryui-touch-punch -->
                'https://cdnjs.cloudflare.com/ajax/libs/jqueryui-touch-punch/0.2.3/jquery.ui.touch-punch.min.js',

                <!-- pivot table -->
                'static/css/pivot.css',
                <%--        <link rel="stylesheet" type="text/css" href="https://cdnjs.cloudflare.com/ajax/libs/pivottable/2.23.0/pivot.min.css">--%>
                'static/js/pivot.js',
                'static/js/pivot.ru.js',

                <!-- math for formulas in pivoting -->
                'https://cdnjs.cloudflare.com/ajax/libs/mathjs/6.2.2/math.min.js',
                'static/js/utils.js',

                <!-- subtotal.js libs : subtotal_renderers -->
                'static/css/subtotal.css',
                <%--        <script type="text/javascript" src="https://cdn.jsdelivr.net/npm/subtotal@1.11.0-alpha.0/dist/subtotal.min.js"></script>--%>
                'static/js/subtotal.js',

                <!--  plotly libs : plotly_renderers  -->
                'https://cdnjs.cloudflare.com/ajax/libs/plotly.js/1.58.4/plotly-basic.min.js',
                'https://cdnjs.cloudflare.com/ajax/libs/plotly.js/1.58.4/plotly-locale-ru.js',

                <%-- will patch plotly_renderers with reverse parameter, since it's makes more sense to show rows on x axis, and columns on y axis --%>
                <%-- + horizontal moved to the end --%>
                'static/js/plotly_renderers.js',
                <%--        <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/pivottable/2.23.0/plotly_renderers.min.js"></script>--%>

                <!--  c3 / d3 libs : d3_renderers -->
                <%--        <link rel="stylesheet" type="text/css" href="https://cdnjs.cloudflare.com/ajax/libs/c3/0.7.11/c3.min.css">--%>
                <%--  because d3_renderers doesn't work with v4+ d3 versions --%>
                'https://cdnjs.cloudflare.com/ajax/libs/d3/3.5.5/d3.min.js',
                <%--        <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/c3/0.7.11/c3.min.js"></script>--%>
                <%--        <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/pivottable/2.23.0/c3_renderers.min.js"></script>--%>
                <%--        <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/pivottable/2.23.0/d3_renderers.min.js"></script>--%>
                'static/js/d3_renderers.js',

                <%--        <!--  google charts: gchart_renderers  -->--%>
                <%--        <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/pivottable/2.23.0/gchart_renderers.min.js"></script>--%>
                <%--        <script type="text/javascript" src="https://www.google.com/jsapi"></script>--%>

                <!--  map  -->
                'https://unpkg.com/leaflet@1.7.1/dist/leaflet.css',
                'https://cdnjs.cloudflare.com/ajax/libs/leaflet.draw/1.0.4/leaflet.draw.css',
                'https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.4.1/MarkerCluster.css',
                'https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.4.1/MarkerCluster.Default.css',
                'https://unpkg.com/leaflet@1.7.1/dist/leaflet.js',
                'https://cdnjs.cloudflare.com/ajax/libs/leaflet.draw/1.0.4/leaflet.draw.js',
                'https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.4.1/leaflet.markercluster.js',
                'https://cdnjs.cloudflare.com/ajax/libs/leaflet-polylinedecorator/1.1.0/leaflet.polylineDecorator.min.js',

                <%--support yandex map tile in leaflet --%>
                ['https://cdnjs.cloudflare.com/ajax/libs/leaflet-plugins/3.4.0/layer/tile/Yandex.min.js', 'leaflet.yandex.plugin.min.js'],
                ['https://cdnjs.cloudflare.com/ajax/libs/leaflet-plugins/3.4.0/layer/tile/Yandex.addon.LoadApi.min.js', 'leaflet.yandex.addon.LoadApi.min.js'],

                <%--support google map tile in leaflet --%>
                ['https://unpkg.com/leaflet.gridlayer.googlemutant@latest/dist/Leaflet.GoogleMutant.js', 'leaflet.GoogleMutant.plugin.js'],

                'static/css/gMap.css',

                <!-- calendar-->
                ['https://cdn.jsdelivr.net/npm/fullcalendar@5.7.2/main.js', 'fullCalendar.js'],
                ['https://cdn.jsdelivr.net/npm/fullcalendar@5.7.2/main.css', 'fullCalendar.css'],
                'static/css/gCalendar.css',
                ['https://cdn.jsdelivr.net/npm/fullcalendar@5.7.2/locales-all.js', 'fullcalendar-locales-all.js'],
                'static/js/fullcalendar-locale-be.js',
                'https://unpkg.com/@popperjs/core@2.5.3/dist/umd/popper.min.js',
                'https://unpkg.com/tippy.js@6.2.7/dist/tippy-bundle.umd.min.js',

                <!-- dateRangePicker -->
                'https://cdn.jsdelivr.net/momentjs/latest/moment.min.js',
                'https://cdn.jsdelivr.net/npm/daterangepicker/daterangepicker.min.js',
                'https://cdn.jsdelivr.net/npm/daterangepicker/daterangepicker.css',

                <!-- Quill -->
                'https://cdn.quilljs.com/1.3.6/quill.js',
                'https://cdn.quilljs.com/1.3.6/quill.bubble.css',
                'static/css/quillRichText.css',

                <!-- Ace code editor -->
                'https://cdnjs.cloudflare.com/ajax/libs/ace/1.4.13/ace.js',
                'https://cdnjs.cloudflare.com/ajax/libs/ace/1.4.13/worker-html.js',
                'https://cdnjs.cloudflare.com/ajax/libs/ace/1.4.13/mode-html.min.js',
                'https://cdnjs.cloudflare.com/ajax/libs/ace/1.4.13/ext-language_tools.min.js',
                'https://cdnjs.cloudflare.com/ajax/libs/ace/1.4.13/theme-chrome.min.js',
                'https://cdnjs.cloudflare.com/ajax/libs/ace/1.4.13/theme-ambiance.min.js',
                'static/css/aceEditor.css'
            ]);

        </script>

        <c:forEach items="${lsfParams}" var="lsfParam">
            <script>
                lsfParams["${lsfParam.key}"] = "${lsfParam.value}";
            </script>
        </c:forEach>

        <c:forEach items="${filesUrls}" var="fileUrl">
            <c:if test="${fileUrl.endsWith('js')}">
                <script type="text/javascript" src="${fileUrl}"></script>
            </c:if>
            <c:if test="${fileUrl.endsWith('css')}">
                <link rel="stylesheet" type="text/css" href="${fileUrl}"/>
            </c:if>
        </c:forEach>

    </head>
    <body>
        <script language="JavaScript">
            var pageSetup = {
                webAppRoot: "<%= request.getContextPath() + "/" %>",
                logicsName: "${logicsName}"
            };
        </script>

        <div id="loadingWrapper">
            <div id="loading" align="center">
                <div class="loadingIndicator">
                    <img id="loadingGif" src="static/images/loading.gif" width="16" height="16"/>
                    lsFusion<br/>
                    <span id="loadingMsg"><%= ServerMessages.getString(request, "loading") %></span>
                </div>
            </div>
        </div>
        <%-- gwt js src is <module name>/<module name>.nocache.js --%>
        <script type="text/javascript" language="javascript"
                src="main/main.nocache.js"></script>
    </body>
</html>
