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
<body onload="document.forgotPassword.username.focus();">

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
                      method="POST">
                    <fieldset>
                        <div class="text-center">
                            <p>
                                <br/>
                                <%= ServerMessages.getString(request, "password.reset") %>
                            </p>
                        </div>
                        <p>
                            <label for="username"><%= ServerMessages.getString(request, "login.or.email") %>
                            </label>
                            <input type="text" id="username" name="username" class="round full-width-box"/>
                        </p>
                        <input name="submit" type="submit" class="button round blue"
                               value="<%= ServerMessages.getString(request, "password.reset") %>"/>
                    </fieldset>
                </form>
                <br>
                <div class="text-center">
                    <a href="/login">На главную</a>
                </div>
            </div>
        </td>
    </tr>
    <tr></tr>
</table>
</body>
</html>