<%@ page import="org.springframework.security.core.context.SecurityContextHolder" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        <meta http-equiv="cache-control" content="no-store, no-cache, must-revalidate"/>
        <meta http-equiv="Pragma" content="no-store, no-cache"/>
        <meta http-equiv="Expires" content="0"/>
        <title>Навигатор</title>
        <link rel="stylesheet" href="style.css">
    </head>
    <body>
        <script language="JavaScript">
            var parameters = {
                userName: <%=
                    SecurityContextHolder.getContext().getAuthentication() != null
                        ? "\"" + SecurityContextHolder.getContext().getAuthentication().getName() + "\""
                        : "undefined"
                  %>
            };
        </script>
        <script> var isomorphicDir = "smartgwt/sc/"; </script>
        <script type="text/javascript" language="javascript" src="smartgwt/smartgwt.nocache.js"></script>
        <script type="text/javascript" language="javascript"
                src="platform.gwt.navigator.Navigator/platform.gwt.navigator.Navigator.nocache.js"></script>
    </body>
</html>
