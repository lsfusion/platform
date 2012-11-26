<%@ page import="platform.gwt.base.server.spring.AccessDeniedHandlerImpl" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title>Ошибка доступа</title>

    <link rel="stylesheet" href="login.css">
    <link rel="shortcut icon" href="favicon.ico" />
</head>
<body>

<table class="content-table">
    <tr></tr>
    <tr>
        <td>
            <div id="content">
                <form id="login-form"
                      action="logout.jsp?targetUrl=<%=URLEncoder.encode(request.getAttribute(AccessDeniedHandlerImpl.ACCESS_DENIED_RESOURCE_URL).toString(), "UTF-8")%>">
                    <fieldset>
                        <p>
                            Доступ к странице ограничен. Вы можете выйти и попробовать войти под другим именем.
                        </p>
                        </p>
                        <input type="submit" class="button round blue image-right ic-left-arrow" value="LOG OUT"/>
                    </fieldset>
                </form>
            </div>
        </td>
    </tr>
    <tr></tr>
</table>
</body>
</html>
