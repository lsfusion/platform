<%@ page import="org.springframework.security.core.context.SecurityContextHolder" %>
<%@ page import="platform.gwt.base.server.ServerUtils" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        <meta http-equiv="cache-control" content="no-store, no-cache, must-revalidate"/>
        <meta http-equiv="Pragma" content="no-store, no-cache"/>
        <meta http-equiv="Expires" content="0"/>
        <meta name="gwt:property" content="locale=<%=ServerUtils.getLocaleLanguage()%>">

        <title>Вход в систему</title>
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

        <jsp:include page="WEB-INF/jsp/smartgwt.jsp"/>

        <script type="text/javascript" language="javascript"
                src="skolkovo.gwt.login.Login/skolkovo.gwt.login.Login.nocache.js"></script>
    </body>
</html>
