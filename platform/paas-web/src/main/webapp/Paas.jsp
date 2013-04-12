<%@ page import="org.springframework.security.core.context.SecurityContextHolder" %>
<%@ page import="platform.gwt.base.server.ServerUtils" %>
<html>
    <head>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
        <meta name="gwt:property" content="locale=<%=ServerUtils.getLocaleLanguage()%>">

        <title>Paas Application</title>

        <!--CSS for loading message at application Startup-->
        <style type="text/css">
            body {
                overflow: hidden
            }

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

            #loadingMsg {
                font: normal 13px arial, tahoma, sans-serif;
            }
        </style>
    </head>

    <body>
        <!-- OPTIONAL: include this if you want history support -->
        <iframe id="__gwt_historyFrame" style="width:0;height:0;border:0"></iframe>

        <!-- RECOMMENDED if your web app will not function without JavaScript enabled -->
        <noscript>
            <div style="width: 22em; position: absolute; left: 50%; margin-left: -11em;
                        color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif">
                Your web browser must have JavaScript enabled
                in order for this application to display correctly.
            </div>
        </noscript>

        <script language="JavaScript">
            var pageSetup = {
                webAppRoot: "<%= request.getContextPath() + "/" %>",
                username: <%=
                            SecurityContextHolder.getContext().getAuthentication() != null
                                ? "\"" + SecurityContextHolder.getContext().getAuthentication().getName() + "\""
                                : "undefined"
                          %>
            };
        </script>

        <!--add loading indicator while the app is being loaded-->
        <div id="loadingWrapper">
            <div id="loading">
                <div class="loadingIndicator">
                    <img src="images/loading.gif" width="16" height="16"
                         style="margin-right:8px;float:left;vertical-align:top;"/>Loading...<br/>
                    <span id="loadingMsg">Loading styles and images...</span></div>
            </div>
        </div>

        <script type="text/javascript">document.getElementById('loadingMsg').innerHTML = 'Loading Core API...';</script>

        <script> var isomorphicDir = "paas/sc/"; </script>

        <!--include the SC Core API-->
        <script src="paas/sc/modules/ISC_Core.js"></script>

        <!--include SmartClient -->
        <script type="text/javascript">document.getElementById('loadingMsg').innerHTML = 'Loading UI Components...';</script>
        <script src='paas/sc/modules/ISC_Foundation.js'></script>
        <script src='paas/sc/modules/ISC_Containers.js'></script>
        <script src='paas/sc/modules/ISC_Grids.js'></script>
        <script src='paas/sc/modules/ISC_Forms.js'></script>
        <script src='paas/sc/modules/ISC_RichTextEditor.js'></script>
        <script src='paas/sc/modules/ISC_Calendar.js'></script>
        <script src='paas/sc/modules/ISC_History.js'></script>
        <script src='paas/sc/modules/ISC_PluginBridges.js'></script>

        <script type="text/javascript">document.getElementById('loadingMsg').innerHTML = 'Loading Data API...';</script>
        <script src='paas/sc/modules/ISC_DataBinding.js'></script>

        <!--load skin-->
        <script type="text/javascript">document.getElementById('loadingMsg').innerHTML = 'Loading skin...';</script>
        <script src='paas/sc/skins/Enterprise/load_skin.js?isc_version=8.1.js'></script>

        <!--include the application JS-->
        <script type="text/javascript">document.getElementById('loadingMsg').innerHTML = 'Loading Application<br>Please wait...';</script>
        <script type="text/javascript" language="javascript" src="paas/paas.nocache.js"></script>
    </body>
</html>
