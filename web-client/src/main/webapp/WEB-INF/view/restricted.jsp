<%@ page import="lsfusion.base.ServerMessages" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>${title}</title>
        <link rel="shortcut icon" href="{$logicsIcon}" />
        <link rel="stylesheet" media="only screen and (min-device-width: 601px)" href="static/noauth/css/login.css"/>
        <link rel="stylesheet" media="only screen and (max-device-width: 600px)" href="static/noauth/css/mobile_login.css"/>
    </head>
    <body onload="document.loginForm.username.focus();">

        <table class="content-table">
            <tr></tr>
            <tr>
                <td>
                    <div id="content">
                        <div class="errorblock round">
                            <%= ServerMessages.getString(request, "login.unsuccessful") %><br/>
                            <%= ServerMessages.getString(request, "login.caused") %>: ${error}
                        </div>
                    </div>
                </td>
            </tr>
            <tr></tr>
        </table>

</body>
</html>