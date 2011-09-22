<%@ page import="org.springframework.security.core.context.SecurityContextHolder" %>
<html>
    <head>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">

        <link type="text/css" rel="stylesheet" href="Paas.css">

        <title>Paas Application</title>

        <!-- You must set the variable isomorphicDir to [MODULE_NAME]/sc/ -->
        <!-- so that the SmartGWT resources are correctly resolved        -->
        <script> var isomorphicDir = "paas/sc/"; </script>

        <script type="text/javascript" language="javascript" src="paas/paas.nocache.js"></script>
    </head>

    <body>
        <script language="JavaScript">
            var parameters = {
                username: <%=
                            SecurityContextHolder.getContext().getAuthentication() != null
                                ? "\"" + SecurityContextHolder.getContext().getAuthentication().getName() + "\""
                                : "undefined"
                          %>
            };
        </script>

        <div id="loading" style="display: block;position: absolute;top: 50%;left: 50%;
         text-align: center;font-family: Tahoma, Verdana, sans-serif;font-size: 11px;">
            <img src="images/loading.gif"/>
            Loading
        </div>

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
    </body>
</html>
