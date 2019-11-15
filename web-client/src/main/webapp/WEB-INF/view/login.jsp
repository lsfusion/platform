<%@ page import="lsfusion.base.ServerMessages" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <title>${title}</title>
        <link rel="shortcut icon" href="${logicsIcon}" />
        <link rel="stylesheet" media="only screen and (min-device-width: 601px)" href="static/noauth/css/login.css"/>
        <link rel="stylesheet" media="only screen and (max-device-width: 600px)" href="static/noauth/css/mobile_login.css"/>
    </head>
    <body onload="document.loginForm.username.focus();">

        <table class="content-table">
            <tr></tr>
            <tr>
                <td>
                    <div id="content">

                        <%
                            String query = request.getQueryString();
                            String queryString = query == null || query.isEmpty() ? "" : ("?" + query);
                        %>

                        <div class="image-center">
                            <img id="logo" class="logo" src="${logicsLogo}" alt="LSFusion">
                        </div>
                        
                        <form id="login-form"
                              name="loginForm"
                              method="POST"
                              action="login_check<%=queryString%>" >
                            <fieldset>
                                <p>
                                    <br/>
                                    <label for="username"><%= ServerMessages.getString(request, "login") %></label>
                                    <input type="text" id="username" name="username" class="round full-width-box"/>
                                </p>
                                <p>
                                    <label for="password"><%= ServerMessages.getString(request, "password") %></label>
                                    <input type="password" id="password" name="password" class="round full-width-box"/>
                                </p>
                                <input name="submit" type="submit" class="button round blue image-right ic-right-arrow" value="<%= ServerMessages.getString(request, "log.in") %>"/>
                                <div class="desktop-link">${jnlpUrls}</div>
                                <c:if test="${not empty SPRING_SECURITY_LAST_EXCEPTION}">
                                    <div class="errorblock round full-width-box">
                                        <%= ServerMessages.getString(request, "login.unsuccessful") %><br/>
                                        <%= ServerMessages.getString(request, "login.caused") %>: ${sessionScope["SPRING_SECURITY_LAST_EXCEPTION"].message}
                                    </div>
                                    <c:remove var="SPRING_SECURITY_LAST_EXCEPTION" scope="session"/>
                                </c:if>
                            </fieldset>
                        </form>
                    </div>
                </td>
            </tr>
            <tr></tr>
        </table>
    </body>
</html>