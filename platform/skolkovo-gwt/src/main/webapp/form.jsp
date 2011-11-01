<%@ page import="platform.gwt.base.server.ServerUtils" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        <meta http-equiv="cache-control" content="no-store, no-cache, must-revalidate"/>
        <meta http-equiv="Pragma" content="no-store, no-cache"/>
        <meta http-equiv="Expires" content="0"/>
        <meta name="gwt:property" content="locale=<%=ServerUtils.getLocaleLanguage()%>">

        <title>Форма</title>
        <link rel="stylesheet" href="style.css">
    </head>
    <body>
        <jsp:include page="WEB-INF/jsp/smartgwt.jsp"/>

        <script type="text/javascript" language="javascript"
                src="skolkovo.gwt.form.Form/skolkovo.gwt.form.Form.nocache.js"></script>
    </body>
</html>
