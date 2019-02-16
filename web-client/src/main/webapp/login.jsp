<%@ page import="lsfusion.base.ServerMessages" %>
<%@ page import="org.springframework.web.servlet.support.RequestContextUtils" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="lsfusion.http.provider.logics.LogicsProvider" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<%-- because we don't use Spring MVC (where we should use controllers (@Controller and component-scan and autowire this bean), put this jsps in WEB-INF/view, and put settings in modelmap), we use standard container JSP dispatch servlet, and using application (servlet) context it's the easiest way to get needed bean --%>
<%
    JSONObject settings = ((LogicsProvider) RequestContextUtils.findWebApplicationContext(request).getBean("logicsProvider")).getServerSettings(request);
%>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>lsFusion</title>

        <link rel="stylesheet" href="login.css">
    </head>
    <body onload="getGUIPreferences(); document.loginForm.username.focus();">

    <script>
        function getGUIPreferences() {

            var xhttp = new XMLHttpRequest();
            xhttp.onload = function() {
                var json = JSON.parse(this.responseText);
                if(json.displayName != null) {
                    document.title = json.displayName;
                }
                document.getElementById("logo").src = json.logicsLogo != null ? ('data:image/jpg;base64,' + json.logicsLogo) : "${pageContext.request.contextPath}/images/logo.png";
                var newLink = document.createElement('link');
                newLink.rel = 'shortcut icon';
                newLink.href = json.logicsIcon != null ? ('data:image/png;base64,'+json.logicsIcon) : 'favicon.ico';
                document.head.appendChild(newLink);
            };
            xhttp.open("GET", "exec?action=System.getGUIPreferences%5B%5D&return=System.GUIPreferences%5B%5D", true);
            xhttp.send();
        }
    </script>

    <div style="visibility: hidden;">448b0ce6-206e-11e9-ab14-d663bd873d93</div>
        <table class="content-table">
            <tr></tr>
            <tr>
                <td>
                    <div id="content">

                        <%
                            String query = request.getQueryString();
                            String queryString = query == null || query.isEmpty() ? "" : ("?" + query);
                        %>

                        <form id="login-form"
                              name="loginForm"
                              method="POST"
                              action="login_check<%=queryString%>" >
                            <fieldset>

                                <div class="image-center"><img id="logo" src="" alt="LSFusion"></div>
                                <p>
                                    <br/>
                                    <label for="username"><%= ServerMessages.getString(request, "login") %></label>
                                    <input type="text" id="username" name="username" class="round full-width-input"/>
                                </p>
                                <p>
                                    <label for="password"><%= ServerMessages.getString(request, "password") %></label>
                                    <input type="password" id="password" name="password" class="round full-width-input"/>
                                </p>
                                <input name="submit" type="submit" class="button round blue image-right ic-right-arrow" value="<%= ServerMessages.getString(request, "log.in") %>"/>
                                <div class="desktop-link">
                                    <span id="triangle" class="triangle" onclick="showSpoiler()">&#9658;</span><a href="${pageContext.request.contextPath}/client.jnlp<%=queryString%>"><%= ServerMessages.getString(request, "run.desktop.client") %></a>
                                    <div id="spoiler" style="display:none"></div>
                                    <script>
                                        function showSpoiler() {
                                            if(document.getElementById('spoiler').style.display==='none') {

                                                var xhttp = new XMLHttpRequest();
                                                xhttp.onload = function() {
                                                    document.getElementById('spoiler').innerHTML = this.responseText.split("{contextPath}").join("${pageContext.request.contextPath}");
                                                };
                                                xhttp.open("GET", "exec?action=Security.generateJnlpUrls%5B%5D&return=Security.jnlpUrls%5B%5D", true);
                                                xhttp.send();
                                                document.getElementById('spoiler') .style.display='';
                                                document.getElementById('triangle').innerHTML = '&#9660;'
                                            } else {
                                                document.getElementById('spoiler') .style.display='none';
                                                document.getElementById('triangle').innerHTML = '&#9658;'
                                            }
                                        }
                                    </script>
                                </div>
                            </fieldset>
                        </form>
                        <c:if test="${!empty param.error}">
                            <div class="errorblock round">
                                <%= ServerMessages.getString(request, "login.unsuccessful") %><br/>
                                <%= ServerMessages.getString(request, "login.caused") %>: ${sessionScope["SPRING_SECURITY_LAST_EXCEPTION"].message}
                            </div>
                        </c:if>
                    </div>
                </td>
            </tr>
            <tr></tr>
        </table>

</body>
</html>