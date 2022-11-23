<%@ page import="lsfusion.base.ServerMessages" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>${title}</title>
    <link rel="shortcut icon" href="${logicsIcon}"/>
    <link rel="stylesheet" media="only screen and (min-device-width: 601px)" href="static/noauth/css/login.css"/>
    <link rel="stylesheet" media="only screen and (max-device-width: 600px)" href="static/noauth/css/mobile_login.css"/>

    <%
        String query = request.getQueryString();
        String queryString = query == null || query.isEmpty() ? "" : ("?" + query);
    %>

    <script>
        let timeToReconnect = 5;
        let interval;

        function init() {
            invertIntervalState();
        }

        function invertIntervalState() {
            let button = document.getElementById("timerAction");
            if (interval == null) {
                button.innerHTML = "<%= ServerMessages.getString(request, "app.server.unavailable.timer.stop") %>";
                interval = setInterval(() => {
                    if (timeToReconnect > 0)
                        timeToReconnect = timeToReconnect - 1;
                    else
                        window.location.href = "/main" + "<%=queryString%>";

                    document.getElementById("reconnectTime").innerHTML = timeToReconnect;
                }, 1000);
            } else {
                button.innerHTML = "<%= ServerMessages.getString(request, "app.server.unavailable.timer.start") %>";
                clearInterval(interval);
                interval = null;
            }
        }
    </script>
</head>
<body onload="init()">
    <div class="main">
        <div class="header">
            <img id="logo" class="logo" src="${logicsLogo}" alt="LSFusion">
            <div class="title">
                <%= ServerMessages.getString(request, "app.server.unavailable") %>
            </div>
        </div>
        <div class="content">
            <div class="appNotAvailable"> ${errorMessage} </div>
            <div class="appNotAvailable appNotAvailableReconnect"><%= ServerMessages.getString(request, "app.server.unavailable.reconnect.after") %> <span id="reconnectTime">5</span> <%= ServerMessages.getString(request, "app.server.unavailable.reconnect.after.seconds") %> <button id="startStopButton" onclick="invertIntervalState()"><span id="timerAction"><%= ServerMessages.getString(request, "app.server.unavailable.timer.stop") %></span> <%= ServerMessages.getString(request, "app.server.unavailable.timer") %></button></div>
        </div>
        <div class="footer">
            <a class="main-page-link link" href="${loginPage}"><%= ServerMessages.getString(request, "login.page") %></a>
        </div>
    </div>
</body>
</html>
