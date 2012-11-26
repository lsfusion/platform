<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>LS Fusion</title>

        <link rel="stylesheet" href="login.css">
    </head>
    <body onload="document.loginForm.j_username.focus();">

    <table class="content-table">
        <tr></tr>
        <tr>
            <td>
                <div id="content">
                    <form name="loginForm" action="j_spring_security_check" method="POST" id="login-form">
                        <fieldset>
                            <p>
                                <label for="j_username">login</label>
                                <input type="text" id="j_username" name="j_username" class="round full-width-input"/>
                            </p>
                            <p>
                                <label for="j_password">password</label>
                                <input type="password" id="j_password" name="j_password" class="round full-width-input"/>
                            </p>
                            <input name="submit" type="submit" class="button round blue image-right ic-right-arrow" value="log in"/>
                            <a class="desktop-link" href="/client.jnlp">
                                Run desktop client
                            </a>
                        </fieldset>
                    </form>
                    <c:if test="${not empty param.error}">
                        <div class="errorblock round">
                            Your login attempt was not successful, try again.<br/> Caused :
                                ${sessionScope["SPRING_SECURITY_LAST_EXCEPTION"].message}
                        </div>
                    </c:if>
                </div>
            </td>
        </tr>
        <tr></tr>
    </table>

    </body>
</html>