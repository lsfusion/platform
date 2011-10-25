<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        <title>Выход из системы</title>
    </head>
    <body>
        <h2>Выполняется выход...</h2><br>
        <%
            String locale = request.getParameter("locale");
            locale = locale == null ? "" : "?locale=" + locale;

            String hostedParams = request.getParameter("gwt.codesvr");
            hostedParams = hostedParams == null
                           ? ""
                           : (locale == null ? "?" : "&") + "gwt.codesvr=" + hostedParams;

            session.invalidate();
            response.sendRedirect("expertProfile.html" + locale + hostedParams);
        %>
    </body>
</html>
