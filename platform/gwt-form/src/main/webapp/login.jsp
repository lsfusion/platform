<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Login LSFForms...</title>
        <style type="text/css">
            .errorblock {
                color: #ff0000;
                background-color: #ffEEEE;
                border: 3px solid #ff0000;
                padding: 8px;
                margin: 16px;
            }
        </style>
    </head>
    <body onload="document.loginForm.j_username.focus();">

        <h3>Login with Username and Password</h3>

        <c:if test="${not empty param.error}">
            <div class="errorblock">
                Your login attempt was not successful, try again.<br /> Caused :
                    ${sessionScope["SPRING_SECURITY_LAST_EXCEPTION"].message}
            </div>
        </c:if>

        <form name="loginForm" action="j_spring_security_check" method="POST">
            <table>
                <tbody>
                <tr>
                    <td><label for="j_username">Username:</label></td>
                    <td><input type="text" id="j_username" name="j_username"/></td>
                </tr>
                <tr>
                    <td><label for="j_password">Password:</label></td>
                    <td><input type="password" id="j_password" name="j_password"/></td>
                </tr>
                </tbody>
            </table>
            <input name="submit" type="submit" value="Login"/>
        </form>
    </body>
</html>