<%@ page import="lsfusion.base.ServerMessages" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>

<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>${title}</title>
    <link rel="shortcut icon" href="${logicsIcon}"/>
    <link rel="stylesheet" media="only screen and (min-device-width: 601px)" href="static/noauth/css/login.css"/>
    <link rel="stylesheet" media="only screen and (max-device-width: 600px)" href="static/noauth/css/mobile_login.css"/>
</head>
<body onload="document.forgotPassword.usernameOrEmail.focus();">

<table class="content-table">
    <tr></tr>
    <tr>
        <td>
            <div id="content">
                <div class="image-center">
                    <img id="logo" class="logo" src="${logicsLogo}" alt="LSFusion">
                </div>
                <form id="forgot-password-form"
                      name="forgotPassword"
                      action="forgot-password"
                      method="POST">
                    <fieldset>
                        <div class="text-center">
                            <p>
                                <br/>
                                <%= ServerMessages.getString(request, "password.reset") %>
                            </p>
                        </div>
                        <p>
                            <label for="usernameOrEmail"><%= ServerMessages.getString(request, "login.or.email") %>
                            </label>
                            <input type="text" id="usernameOrEmail" name="usernameOrEmail" class="round full-width-box"/>
                        </p>
                        <input name="submit" type="submit" class="button round blue"
                               value="<%= ServerMessages.getString(request, "password.reset") %>"/>
                        <c:if test="${not empty RESET_PASSWORD_EXCEPTION}">
                            <div class="errorblock round full-width-box">
                                    ${sessionScope["RESET_PASSWORD_EXCEPTION"]}
                            </div>
                            <c:remove var="RESET_PASSWORD_EXCEPTION" scope="session"/>
                        </c:if>
                    </fieldset>
                </form>
                <br>
                <div class="text-center">
                    <a class="main-page-link" href="${loginPage}"><%= ServerMessages.getString(request, "main.page") %></a>
                </div>
            </div>
        </td>
    </tr>
    <tr></tr>
</table>
</body>
</html>