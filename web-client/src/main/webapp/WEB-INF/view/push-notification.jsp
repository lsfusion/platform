<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title></title>
    <script>
        function init() {
            navigator.serviceWorker.addEventListener("message", (event) => {
                if(event.data === 'close') {
                    window.open('', '_self');
                    window.close();
                }
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
<body onload="init();">
</body>
</html>
