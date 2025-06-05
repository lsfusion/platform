<%@ page import="lsfusion.base.ServerMessages" %>
<%@ page import="lsfusion.base.ServerUtils" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="lsf" uri="writeResources" %>

<!DOCTYPE html>
<html>
    <head>
        <link rel="manifest" href="manifest">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <title>${title}</title>
        <lsf:writeResources resources="${resourcesBeforeSystem}"/>
        <link rel="shortcut icon" href="${logicsIcon}" />
        <link rel="stylesheet" media="only screen and (min-device-width: 601px)" href="static/noauth/css/login.css"/>
        <link rel="stylesheet" media="only screen and (max-device-width: 600px)" href="static/noauth/css/mobile_login.css"/>

        <% pageContext.setAttribute("versionedResources", ServerUtils.getVersionedResources(config.getServletContext(),

                "static/noauth/css/fontAwesome/css/fontawesome.min.css",
                "static/noauth/css/fontAwesome/css/brands.min.css",
                "static/noauth/css/fontAwesome/css/solid.min.css"
                )); %>
        <lsf:writeResources resources="${versionedResources}"/>
        <lsf:writeResources resources="${resourcesAfterSystem}"/>

    </head>
    <body onload="if (document.loginForm && document.loginForm.username) document.loginForm.username.focus();">
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

                <c:if test="${empty sessionScope['2fa_code']}">
                    <form id="login-form"
                          name="loginForm"
                          method="POST"
                          action="login_check<%=queryString%>" >
                        <fieldset>
                            <div class="label-and-field">
                                <label for="username"><%= ServerMessages.getString(request, "login.or.email") %></label>
                                <input autocapitalize="off" type="text" id="username" name="username" class="round full-width-box" value="${username}"/>
                            </div>
                            <div class="label-and-field">
                                <div class="password-labels-container">
                                    <label for="password"><%= ServerMessages.getString(request, "password") %></label>
                                    <a class="link" href="${forgotPasswordPage}" tabindex="1"><%= ServerMessages.getString(request, "password.forgot") %></a>
                                </div>
                                <input type="password" id="password" name="password" class="round full-width-box"/>
                            </div>
                            <input name="submit" type="submit" class="action-button round blue" value="<%= ServerMessages.getString(request, "sign.in") %>"/>
                        </fieldset>
                    </form>
                    <c:if test="${not empty SPRING_SECURITY_LAST_EXCEPTION}">
                        <%
                            if (session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION") instanceof Exception) {
                                session.setAttribute("SPRING_SECURITY_LAST_EXCEPTION", ((Exception) session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION")).getMessage());
                            }
                        %>
                        <div class="error-block round">
                                ${SPRING_SECURITY_LAST_EXCEPTION}
                        </div>
                        <c:remove var="SPRING_SECURITY_LAST_EXCEPTION" scope="session"/>
                    </c:if>
                    <c:if test="${! disableRegistration}">
                        <div class="reg-block">
                            <%= ServerMessages.getString(request, "no.account") %>
                            &#32;
                            <a class="link" href="${registrationPage}"><%= ServerMessages.getString(request, "register") %></a>.
                        </div>
                    </c:if>
                    <c:if test="${not empty urls}">
                        <div class="oauth-block">
                            <div class="oauth-title"><%= ServerMessages.getString(request, "sign.in.with") %></div>
                            <div class="oauth-links">
                                <c:forEach var="url" items="${urls}">
                                    <a href="${url.value}" class="oauth-link fa-brands fa-solid fa-${url.key}" title="${url.key}"></a>
                                </c:forEach>
                            </div>
                        </div>
                    </c:if>
                </c:if>
                <c:if test="${not empty sessionScope['2fa_code']}">
                    <form id="submit-2fa-form" method="POST" action="2fa<%=queryString%>">
                        <div class="label-and-field">
                            <label for="code"><%= ServerMessages.getString(request, "two.fa.code") %></label>
                            <input class="full-width-box" id="code" type="text" name="code" maxlength="6" oninput="this.value = this.value.replace(/[^0-9]/g, '')" inputmode="numeric" />

                            <c:if test="${not empty sessionScope['2fa_error']}">
                                <div style="color:red;"><%= ServerMessages.getString(request, "two.fa.error") %></div>
                                <c:remove var="2fa_error" scope="session"/>
                            </c:if>
                        </div>

                        <input type="submit" class="action-button round blue" value="<%= ServerMessages.getString(request, "password.new.confirm") %>">
                    </form>
                </c:if>
            </div>
            <div class="footer">
                <c:if test="${empty sessionScope['2fa_code']}">
                    <div class="desktop-link link">
                        ${jnlpUrls}
                    </div>
                </c:if>
                <c:if test="${not empty sessionScope['2fa_code']}">
                    <form id="cancel-2fa-form" method="POST" action="2fa<%=queryString%>">
                        <input type="hidden" name="cancel" value="true"/>

                        <input type="submit" class="cancel-two-factor main-page-link" value="<%= ServerMessages.getString(request, "login.page") %>" >
                    </form>
                </c:if>
                <div class="client-version">
                    ${apiVersion}
                </div>
            </div>
        </div>
    </body>
</html>