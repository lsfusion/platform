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
        <link rel="stylesheet" href="login.css">
    </head>
    <body onload="document.loginForm.username.focus();">

    <div style="visibility: hidden;">448b0ce6-206e-11e9-ab14-d663bd873d93</div>
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