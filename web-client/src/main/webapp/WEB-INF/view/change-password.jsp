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
        <script>
            const check = function () {
                if (document.getElementById('newPassword').value !==
                    document.getElementById('repeatPassword').value) {
                    document.getElementById('message').style.color = 'red';
                    document.getElementById('message').innerHTML = '<%= ServerMessages.getString(request, "password.not.match") %>';
                    document.getElementById('submit').disabled = true;
                } else {
                    document.getElementById('submit').disabled = false;
                    document.getElementById('message').innerHTML = '';
                }
            };
        </script>
    </head>
    <body onload="document.setNewPassword.newPassword.focus();">

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
                    <form id="new-password-form"
                          name="setNewPassword"
                          action="change-password<%=queryString%>"
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
                                <input type="password" id="newPassword" name="newPassword" class="round full-width-box"
                                       required onkeyup='check();'/>
                            </p>
                            <p>
                                <label for="repeatPassword"><%= ServerMessages.getString(request, "password.repeat") %>
                                </label>
                                <input type="password" id="repeatPassword" name="repeatPassword" class="round full-width-box"
                                       required onkeyup='check();'/>
                                <span id='message'></span>
                            </p>
                            <input name="submit" type="submit" class="button round blue" id="submit" disabled
                                   value="<%= ServerMessages.getString(request, "password.new.confirm") %>"/>
                            <c:if test="${not empty SPRING_SECURITY_LAST_EXCEPTION}">
                                <div class="errorblock round full-width-box">
                                        ${SPRING_SECURITY_LAST_EXCEPTION}
                                </div>
                                <c:remove var="SPRING_SECURITY_LAST_EXCEPTION" scope="session"/>
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