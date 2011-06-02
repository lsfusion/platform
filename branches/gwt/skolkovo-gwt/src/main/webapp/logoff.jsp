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

        session.invalidate();
        response.sendRedirect("expertProfile.html" + locale);
    %>
  </body>
</html>
