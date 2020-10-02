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
                if (document.getElementById('password').value !==
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
    <body onload="document.registrationForm.username.focus();">
        <div class="main">
            <div class="header">
                <img id="logo" class="logo" src="${logicsLogo}" alt="LSFusion">
                <div class="title">
                    <%= ServerMessages.getString(request, "registration") %>
                </div>
            </div>
            <div class="content">

                <%
                    String query = request.getQueryString();
                    String queryString = query == null || query.isEmpty() ? "" : ("?" + query);
                %>

                <form id="registration-form"
                      action="registration<%=queryString%>"
                      name="registrationForm"
                      method="POST">
                    <fieldset>
                        <div class="label-and-field">
                            <label for="username"><%= ServerMessages.getString(request, "login") %></label>
                            <input type="text" id="username" name="username" class="round full-width-box" required="required"/>
                        </div>
                        <div class="label-and-field">
                            <label for="password"><%= ServerMessages.getString(request, "password") %></label>
                            <input type="password" id="password" name="password" class="round full-width-box" required onkeyup='check();'/>
                        </div>
                        <div class="label-and-field">
                            <label for="repeatPassword"><%= ServerMessages.getString(request, "password.repeat") %></label>
                            <input type="password" id="repeatPassword" name="repeatPassword" class="round full-width-box" required onkeyup='check();'/>
                            <span id='message'></span>
                        </div>
                        <div class="label-and-field">
                            <label for="firstName"><%= ServerMessages.getString(request, "first.name") %></label>
                            <input type="text" id="firstName" name="firstName" class="round full-width-box" required="required"/>
                        </div>
                        <div class="label-and-field">
                            <label for="lastName"><%= ServerMessages.getString(request, "last.name") %></label>
                            <input type="text" id="lastName" name="lastName" class="round full-width-box" required="required"/>
                        </div>
                        <div class="label-and-field">
                            <label for="email"><%= ServerMessages.getString(request, "email") %></label>
                            <input type="email" id="email" name="email" class="round full-width-box" required="required"/>
                        </div>
                        <input name="submit" type="submit" class="action-button round blue" id="submit" value="<%= ServerMessages.getString(request, "registration") %>"/>
                        <c:if test="${not empty REGISTRATION_EXCEPTION}">
                            <div class="error-block round full-width-box">
                                <%= ServerMessages.getString(request, "registration.not.successful") %><br/>
                                <%= ServerMessages.getString(request, "login.caused") %>: ${sessionScope["REGISTRATION_EXCEPTION"].message}
                            </div>
                            <c:remove var="REGISTRATION_EXCEPTION" scope="session"/>
                        </c:if>
                    </fieldset>
                </form>
            </div>
            <div class="footer">
                <a class="main-page-link link" href="${loginPage}"><%= ServerMessages.getString(request, "main.page") %></a>
            </div>
        </div>
    </body>
</html>