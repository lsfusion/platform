<%@ page import="lsfusion.base.ServerMessages" %>
<%@ page import="lsfusion.base.ServerUtils" %>
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

        <% pageContext.setAttribute("fontAwesome", ServerUtils.getVersionedResource(config.getServletContext(), "static/noauth/css/fontAwesome/css/font-awesome.min.css")); %>
        <link rel='stylesheet' type='text/css' href='${fontAwesome}' />

    </head>
    <body onload="document.loginForm.username.focus();">
        <div class="main">
            <div class="header">
                <img id="logo" class="logo" src="${logicsLogo}" alt="LSFusion">
                <div class="title">
                    <%= ServerMessages.getString(request, "sign.in.title") %>
                </div>
            </div>
            <div class="content">

                <%
                    String query = request.getQueryString();
                    String queryString = query == null || query.isEmpty() ? "" : ("?" + query);
                %>

                <form id="login-form"
                      name="loginForm"
                      method="POST"
                      action="login_check<%=queryString%>" >
                    <fieldset>
                        <div class="label-and-field">
                            <label for="username"><%= ServerMessages.getString(request, "login") %></label>
                            <input type="text" id="username" name="username" class="round full-width-box"/>
                        </div>
                        <div class="label-and-field">
                            <div class="password-labels-container">
                                <label for="password"><%= ServerMessages.getString(request, "password") %></label>
                                <a class="link" href="${forgotPasswordPage}" tabindex="1"><%= ServerMessages.getString(request, "password.forgot") %></a>
                            </div>
                            <input type="password" id="password" name="password" class="round full-width-box"/>
                        </div>
                        <input name="submit" type="submit" class="action-button round blue" value="<%= ServerMessages.getString(request, "sign.in") %>"/>
                        <c:if test="${not empty SPRING_SECURITY_LAST_EXCEPTION}">
                            <%
                                if (session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION") instanceof Exception) {
                                    session.setAttribute("SPRING_SECURITY_LAST_EXCEPTION", ((Exception) session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION")).getMessage());
                                }
                            %>
                            <div class="error-block round full-width-box">
                                ${SPRING_SECURITY_LAST_EXCEPTION}
                            </div>
                            <c:remove var="SPRING_SECURITY_LAST_EXCEPTION" scope="session"/>
                        </c:if>
                    </fieldset>
                </form>
                <c:if test="${! disableRegistration}">
                    <div class="reg-block">
                        <%= ServerMessages.getString(request, "no.account") %>
                        &#32;
                        <a class="link" href="${registrationPage}"><%= ServerMessages.getString(request, "register") %></a>.
                    </div>
                    <c:if test="${not empty urls}">
                        <div class="oauth-block">
                            <div class="oauth-title"><%= ServerMessages.getString(request, "sign.in.with") %></div>
                            <div class="oauth-links">
                                <c:forEach var="url" items="${urls}">
                                    <a href="${url.value}" class="oauth-link fa fa-${url.key}" title="${url.key}"></a>
                                </c:forEach>
                            </div>
                        </div>
                    </c:if>
                </c:if>
            </div>
            <div class="footer">
                <div class="desktop-link link">
                    ${jnlpUrls}
                </div>
            </div>
        </div>
    </body>
</html>