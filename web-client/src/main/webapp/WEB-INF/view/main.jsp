<%@ page import="lsfusion.base.ServerMessages" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="lsfusion.base.ServerUtils" %>
<%@ taglib prefix="lsf" uri="writeResources" %>

<!DOCTYPE html>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <link rel="manifest" href="manifest">
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    	<meta name="viewport" content="width=device-width, initial-scale=1">

        <title>${title}</title>
        <link rel="shortcut icon" href="${logicsIcon}" />

        <% pageContext.setAttribute("themeCSS", ServerUtils.getVersionedResource(config.getServletContext(), "static/css/theme/light.css")); %>
        <link id="themeCss" rel="stylesheet" type="text/css" href="${themeCSS}"/>

        <style>
            @import url('static/css/fontsGoogle/fonts_googleapis_OpenSans.css');
        </style>

        <style>
            .loadingIndicator {
                position: absolute;
                top: 50%;
                left: 50%;
                z-index: 20001;
                transform: translate(-50%, -50%);
            }

            .loading-img {
                height: 5rem;
                width: 5rem;
            }

        </style>

        <lsf:writeResources resources="${mainResourcesBeforeSystem}"/>

        <% pageContext.setAttribute("versionedResources", ServerUtils.getVersionedResources(config.getServletContext(),
                "static/js/external/jquery-3.7.1.min.js",
                "static/js/external/jquery-ui.min.js",
                "static/css/external/jquery-ui.min.css",

                //export pivot to excel
                "static/js/tableToExcel.js",

                "static/noauth/css/fontAwesome/css/fontawesome.min.css",
                "static/noauth/css/fontAwesome/css/brands.min.css",
                "static/noauth/css/fontAwesome/css/solid.min.css",
                "static/noauth/css/fontAwesome/css/regular.min.css",
                "static/noauth/css/lsfFonts/lsf-icons.css",

                //optional: mobile support with jqueryui-touch-punch
                "static/js/external/jquery.ui.touch-punch.min.js", //https://cdnjs.cloudflare.com/ajax/libs/jqueryui-touch-punch/0.2.3/jquery.ui.touch-punch.min.js

                //pivot table
                "static/css/gwt/pivot.css",
                //<link rel="stylesheet" type="text/css" href="https://cdnjs.cloudflare.com/ajax/libs/pivottable/2.23.0/pivot.min.css">
                "static/js/pivot.js",
                "static/js/pivot.ru.js",

                //math for formulas in pivoting
                "static/js/external/math.min.js", //https://cdnjs.cloudflare.com/ajax/libs/mathjs/6.2.2/math.min.js
                "static/js/utils.js",

                //subtotal.js libs : subtotal_renderers
                "static/css/subtotal.css",
                //<script type="text/javascript" src="https://cdn.jsdelivr.net/npm/subtotal@1.11.0-alpha.0/dist/subtotal.min.js"></script>
                "static/js/subtotal.js",

                //plotly libs : plotly_renderers
                "static/js/external/plotly-basic.min.js", //https://cdnjs.cloudflare.com/ajax/libs/plotly.js/1.58.4/plotly-basic.min.js
                "static/js/external/plotly-locale-ru.js", //https://cdnjs.cloudflare.com/ajax/libs/plotly.js/1.58.4/plotly-locale-ru.js

                //will patch plotly_renderers with reverse parameter, since it's makes more sense to show rows on x axis, and columns on y axis
                //+ horizontal moved to the end
                "static/js/plotly_renderers.js",
                //https://cdnjs.cloudflare.com/ajax/libs/pivottable/2.23.0/plotly_renderers.min.js

                //c3 / d3 libs : d3_renderers
                //https://cdnjs.cloudflare.com/ajax/libs/c3/0.7.11/c3.min.css
                //because d3_renderers doesn't work with v4+ d3 versions
                "static/js/external/d3.min.js", //https://cdnjs.cloudflare.com/ajax/libs/d3/3.5.5/d3.min.js
                //<script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/c3/0.7.11/c3.min.js"></script>
                //<script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/pivottable/2.23.0/c3_renderers.min.js"></script>
                //<script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/pivottable/2.23.0/d3_renderers.min.js"></script>
                "static/js/d3_renderers.js",

                //google charts: gchart_renderers
                //<script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/pivottable/2.23.0/gchart_renderers.min.js"></script>
                //<script type="text/javascript" src="https://www.google.com/jsapi"></script>

                //map
                "static/css/external/leaflet.css", //https://unpkg.com/leaflet@1.9.4/dist/leaflet.css
                "static/css/external/leaflet.draw.css", //https://cdnjs.cloudflare.com/ajax/libs/leaflet.draw/1.0.4/leaflet.draw.css
                "static/css/external/MarkerCluster.css", //https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.4.1/MarkerCluster.css
                "static/css/external/MarkerCluster.Default.css", //https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.4.1/MarkerCluster.Default.css
                "static/js/external/leaflet.js", //https://unpkg.com/leaflet@1.9.4/dist/leaflet.js
                "static/js/external/leaflet.draw.js", //https://cdnjs.cloudflare.com/ajax/libs/leaflet.draw/1.0.4/leaflet.draw.js
                "static/js/external/leaflet.markercluster.js", //https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.4.1/leaflet.markercluster.js
                "static/js/external/leaflet.polylineDecorator.min.js", //https://cdnjs.cloudflare.com/ajax/libs/leaflet-polylinedecorator/1.1.0/leaflet.polylineDecorator.min.js

                //support yandex map tile in leaflet
                "static/js/external/leaflet.yandex.plugin.min.js", //https://cdnjs.cloudflare.com/ajax/libs/leaflet-plugins/3.4.0/layer/tile/Yandex.min.js
                "static/js/external/leaflet.yandex.addon.LoadApi.min.js", //https://cdnjs.cloudflare.com/ajax/libs/leaflet-plugins/3.4.0/layer/tile/Yandex.addon.LoadApi.min.js

                //support google map tile in leaflet
                "static/js/external/leaflet.GoogleMutant.plugin.js", //https://unpkg.com/leaflet.gridlayer.googlemutant@latest/dist/Leaflet.GoogleMutant.js

                "static/css/gMap.css",

                //calendar
                "static/js/external/fullcalendar.index.global.min.js", //https://cdn.jsdelivr.net/npm/fullcalendar@6.1.8/index.global.min.js
                "static/css/gCalendar.css",
                "static/js/external/fullcalendar-locales-all.js", //https://cdn.jsdelivr.net/npm/fullcalendar@5.7.2/locales-all.js
                "static/js/fullcalendar-locale-be.js",
                "static/js/external/popper.min.js", //https://unpkg.com/@popperjs/core@2.11.8/dist/umd/popper.min.js
                "static/js/external/tippy-bundle.umd.min.js", //https://unpkg.com/tippy.js@6.3.7/dist/tippy-bundle.umd.min.js
                "static/css/external/tippy.css",

                //dateRangePicker
                "static/js/external/moment-with-locales.min.js", //https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.29.4/moment-with-locales.min.js // also used to define user date / time formats
                "static/js/external/moment-jdateformatparser.min.js", //https://raw.githubusercontent.com/MadMG/moment-jdateformatparser/master/moment-jdateformatparser.min.js // also used to define user date / time formats
                "static/js/external/daterangepicker.js",
                "static/css/external/daterangepicker.css", //https://cdn.jsdelivr.net/npm/daterangepicker@3.1.0/daterangepicker.css
                "static/css/datePicker.css",

                //Quill
                "static/js/external/quill.min.js", //https://cdn.quilljs.com/1.3.6/quill.min.js
                "static/css/external/quill.bubble.css", //https://cdn.quilljs.com/1.3.6/quill.bubble.css
                "static/css/quillRichText.css",

                //Ace code editor
                //todo minification is needed because the ace library size is larger than 5mb
                "static/js/ace/src/ace.js",
                "static/js/ace/src/mode-html.js",
                "static/js/ace/src/mode-lsf.js",
                "static/js/ace/src/mode-java.js",
                "static/js/ace/src/worker-html.js",
                "static/js/ace/src/worker-lsf.js",
                "static/js/ace/src/ext-language_tools.js",
                "static/js/ace/src/theme-chrome.js",
                "static/js/ace/src/theme-ambiance.js",

                "static/js/external/jquery.inputmask.min.js",

                //MMenuLight
                "static/js/external/mmenu-light.js", //https://cdn.jsdelivr.net/npm/mmenu-light@3.2.0/dist/mmenu-light.js
                "static/css/external/mmenu-light.css", //https://cdn.jsdelivr.net/npm/mmenu-light@3.2.0/dist/mmenu-light.css
                "static/css/mmenu.css",

                "static/css/bootstrap-icons/bootstrap-icons.css",

                "static/css/gwt/main.css",

                "static/css/gwt/form/components/base/dialog.css",
                "static/css/gwt/form/components/base/popup-menu.css",
                "static/css/gwt/form/components/base/progress-bar.css",
                "static/css/gwt/form/components/base/resizable-window.css",
                "static/css/gwt/form/components/base/imagetext.css",

                "static/css/gwt/form/components/property/property.css",
                "static/css/gwt/form/components/property/property-toolbar.css",

                "static/css/gwt/form/components/table/table.css",
                "static/css/gwt/form/components/table/table-arrow.css",
                "static/css/gwt/form/components/table/table-header.css",
                "static/css/gwt/form/components/table/table-cell.css",
                "static/css/gwt/form/components/table/table-container.css",
                "static/css/gwt/form/components/table/table-sticky.css",
                "static/css/gwt/form/components/table/tree.css",

                "static/css/gwt/form/components/filter.css",
                "static/css/gwt/form/components/property/panel-renderer.css",
                "static/css/gwt/form/components/toolbar.css",
                "static/css/gwt/form/components/user-preferences.css",

                "static/css/gwt/form/layout/caption-panel.css",
                "static/css/gwt/form/layout/layout.css",
                "static/css/gwt/form/layout/tab-panel.css",

                "static/css/gwt/navigator/navigator.css",
                "static/css/gwt/navigator/split-window.css",
                "static/css/gwt/navigator/tab-window.css",
                "static/css/gwt/option.css",
                "static/css/gwt/htmlortext.css"
        ));%>

        <lsf:writeResources resources="${versionedResources}"/>
        <lsf:writeResources resources="${mainResourcesAfterSystem}"/>

        <c:forEach items="${lsfParams}" var="lsfParam">
            <script>
                lsfParams["${lsfParam.key}"] = "${lsfParam.value}";
            </script>
        </c:forEach>

        <script>
            function init() {
                //setClientLocaleParams
                let intlOptions = Intl.DateTimeFormat().resolvedOptions();
                setCookie('LSFUSION_CLIENT_TIME_ZONE', intlOptions.timeZone);

                let momentLocale = moment();
                momentLocale.locale(intlOptions.locale);
                setCookie('LSFUSION_CLIENT_TIME_FORMAT', momentLocale.toJDFString(momentLocale.localeData().longDateFormat('LT')));
                setCookie('LSFUSION_CLIENT_DATE_FORMAT', momentLocale.toJDFString(momentLocale.localeData().longDateFormat('L')));

                setColorTheme();

                setCookie('LSFUSION_SCREEN_SIZE', document.documentElement.clientWidth + 'x' + document.documentElement.clientHeight);
                setCookie('LSFUSION_SCALE', window.devicePixelRatio);

                try {
                    navigator.serviceWorker.ready.then((registration) => {
                        registration.active.postMessage({
                            type: "setDefaultNotifyOptions",
                            defaultNotification: {
                                title: "${title}",
                                options: {
                                    icon: "${logicsIcon}",
                                    body: "<%= ServerMessages.getString(request, "executed.successfully") %>"
                                }
                            },
                            focusNotification: {
                                title: "<%= ServerMessages.getString(request, "push.notification.tab.already.opened") %>",
                                options: {
                                    body: "<%= ServerMessages.getString(request, "push.notification.can.close.tab") %>"
                                }
                            }
                        });
                    });
                } catch (error) {
                    console.warn(error)
                }
            }
        </script>
    </head>
    <body onload="init();" sessionID = '${sessionID}'>
        <script language="JavaScript">
            var pageSetup = {
                webAppRoot: "<%= request.getContextPath() + "/" %>",
                logicsName: "${logicsName}"
            };
        </script>

        <div class="loadingIndicator" id="loadingWrapper">
            <img class="loading-img" src="static/images/loading.gif" alt="Loading..."/>
        </div>

        <%-- gwt js src is <module name>/<module name>.nocache.js --%>
        <script type="text/javascript" language="javascript"
                src="main/main.nocache.js"></script>
    </body>
</html>
