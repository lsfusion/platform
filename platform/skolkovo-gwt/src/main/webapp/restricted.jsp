<%@ page import="java.net.URLEncoder" %>
<%@ page import="platform.gwt.base.server.spring.AccessDeniedHandlerImpl" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title>Ошибка доступа</title>
  </head>
  <body>
    <p>
        Доступ к странице ограничен. Вы можете выйти и попробовать войти под другим именем.
        </p>
        <a href="logout?targetUrl=<%=URLEncoder.encode(request.getAttribute(AccessDeniedHandlerImpl.ACCESS_DENIED_RESOURCE_URL).toString(), "UTF-8")%>">
            Выйти
        </a>
    </p>
  </body>
</html>
