<%@ page import="lsfusion.base.ServerMessages" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>${title}</title>
    <link rel="shortcut icon" href="${logicsIcon}"/>
    <link rel="stylesheet" media="only screen and (min-device-width: 601px)" href="static/noauth/css/login.css"/>
    <link rel="stylesheet" media="only screen and (max-device-width: 600px)" href="static/noauth/css/mobile_login.css"/>

    <script>
        function init() {
            navigator.serviceWorker.addEventListener("message", (event) => {
                if (event.data === 'close')
                    document.getElementById("push-notification").style.display = 'block'
            });
            navigator.serviceWorker.register('service-worker.js');
            navigator.serviceWorker.ready.then((registration) => {
                registration.active.postMessage({
                    type: 'pushNotification',
                    actionId: ${id},
                    push: {
                        query: "${query}"
                    }
                });
            });
        }
    </script>
</head>
<body onload="init();" id="push-notification" style="display: none">
    <div class="main">
        <div class="header">
            <img id="logo" class="logo" src="${logicsLogo}" alt="LSFusion">
            <div class="title">
                <%= ServerMessages.getString(request, "push.notification.title") %>
            </div>
        </div>
        <div class="content">
            <div class="tab-already-opened"> <%= ServerMessages.getString(request, "push.notification.tab.already.opened") %> </div>
            <div class="tab-already-opened can-close-tab"> <%= ServerMessages.getString(request, "push.notification.can.close.tab") %> </div>
        </div>
        <div class="footer"></div>
    </div>
</body>
</html>
