<%@ page import="platform.gwt.base.server.ServerUtils" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        <meta http-equiv="Pragma" content="no-cache">
        <meta name="gwt:property" content="locale=<%=ServerUtils.getLocaleLanguage()%>">
        <title>Анкета участника фонда</title>
        <link rel="stylesheet" href="style.css">
    </head>
    <body>
        <jsp:include page="WEB-INF/jsp/smartgwt.jsp"/>

        <script type="text/javascript" language="javascript"
                src="skolkovo.gwt.claimer.Claimer/skolkovo.gwt.claimer.Claimer.nocache.js"></script>
    </body>
</html>
