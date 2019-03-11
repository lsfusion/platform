<%@ page import="lsfusion.base.ServerMessages" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>${title}</title>
        <link rel="shortcut icon" href="${logicsIcon}" />
        <link rel="stylesheet" href="static/noauth/login.css">
    </head>
    <body onload="document.loginForm.username.focus();">

    <div style="visibility: hidden;">448b0ce6-206e-11e9-ab14-d663bd873d93</div>
        <table class="content-table">
            <tr></tr>
            <tr>
                <td>
                    <div id="content">

                        <%
                            String query = request.getQueryString();
                            String queryString = query == null || query.isEmpty() ? "" : ("?" + query);
                        %>

                        <form id="login-form"
                              name="loginForm"
                              method="POST"
                              action="login_check<%=queryString%>" >
                            <fieldset>

                                <div class="image-center"><img id="logo" src="${logicsLogo}" alt="LSFusion"></div>
                                <p>
                                    <br/>
                                    <label for="username"><%= ServerMessages.getString(request, "login") %></label>
                                    <input type="text" id="username" name="username" class="round full-width-input"/>
                                </p>
                                <p>
                                    <label for="password"><%= ServerMessages.getString(request, "password") %></label>
                                    <input type="password" id="password" name="password" class="round full-width-input"/>
                                </p>
                                <input name="submit" type="submit" class="button round blue image-right ic-right-arrow" value="<%= ServerMessages.getString(request, "log.in") %>"/>
                                <div class="desktop-link">${jnlpUrls}</div>
                            </fieldset>
                        </form>
                        <c:if test="${not empty SPRING_SECURITY_LAST_EXCEPTION}">
                            <div class="errorblock round">
                                <%= ServerMessages.getString(request, "login.unsuccessful") %><br/>
                                <%= ServerMessages.getString(request, "login.caused") %>: ${sessionScope["SPRING_SECURITY_LAST_EXCEPTION"].message}
                            </div>
                            <c:remove var="SPRING_SECURITY_LAST_EXCEPTION" scope="session"/>
                        </c:if>
                    </div>
                </td>
            </tr>
            <tr></tr>
        </table>

</body>
</html>