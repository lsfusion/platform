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
<body onload="document.newPassword.newPassword.focus();">

<table class="content-table">
    <tr></tr>
    <tr>
        <td>
            <div id="content">
                <div class="image-center">
                    <img id="logo" class="logo" src="${logicsLogo}" alt="LSFusion">
                </div>
                <form id="new-password-form"
                      name="newPassword"
                      method="POST">
                    <fieldset>
                        <div class="text-center">
                            <p>
                                <br/>
                                <%= ServerMessages.getString(request, "password.new") %>
                            </p>
                        </div>
                        <p>
                            <label for="newPassword"><%= ServerMessages.getString(request, "password") %>
                            </label>
                            <input type="password" id="newPassword" name="newPassword" class="round full-width-box"/>
                        </p>
                        <p>
                            <label for="repeatPassword"><%= ServerMessages.getString(request, "password.repeat") %>
                            </label>
                            <input type="password" id="repeatPassword" name="repeatPassword" class="round full-width-box"/>
                        </p>
                        <input name="submit" type="submit" class="button round blue"
                               value="<%= ServerMessages.getString(request, "password.new.confirm") %>"/>
                    </fieldset>
                </form>
                <br>
            </div>
        </td>
    </tr>
    <tr></tr>
</table>
</body>
</html>