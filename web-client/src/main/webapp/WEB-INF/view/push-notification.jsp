<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title></title>
    <script>
        function init() {
            let notification = ${notificationId};
            let timeoutId = setTimeout(function() {
                window.location.replace("${redirectUrl}");
            }, 100);

            let broadcastChannel = new BroadcastChannel("${notificationChannel}");
            broadcastChannel.addEventListener("message", (event) => {
                if(event.data.startsWith("${notificationReceived}" + notification)) {
                    window.open('', '_self');
                    window.close();
                    clearTimeout(timeoutId);
                }
            });
            broadcastChannel.postMessage("${notificationSend}" + notification);
        }
    </script>
</head>
<body onload="init();">
</body>
</html>
