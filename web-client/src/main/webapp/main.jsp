<%@ page import="lsfusion.base.ServerMessages" %>

<!DOCTYPE html>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        
        <title>lsFusion</title>
        <style type="text/css">
            #loading {
                border: 1px solid #ccc;
                position: absolute;
                left: 45%;
                top: 40%;
                padding: 2px;
                z-index: 20001;
                height: auto;
            }

            #loading a {
                color: #225588;
            }

            #loading .loadingIndicator {
                background: white;
                font: bold 13px tahoma, arial, helvetica;
                padding: 10px;
                margin: 0;
                height: auto;
                color: #444;
            }

            #loadingGif {
                vertical-align:top;
            }

            #loadingMsg {
                font: normal 13px arial, tahoma, sans-serif;
            }
        </style>
    </head>
    <body onload="getGUIPreferences()">
        <script language="JavaScript">
            var pageSetup = {
                webAppRoot: "<%= request.getContextPath() + "/" %>"
            };

            function getGUIPreferences() {

                var xhttp = new XMLHttpRequest();
                xhttp.onload = function() {
                    var json = JSON.parse(this.responseText);
                    if(json.displayName != null) {
                        document.title = json.displayName;
                    }
                    var newLink = document.createElement('link');
                    newLink.rel = 'shortcut icon';
                    newLink.href = json.logicsIcon != null ? ('data:image/png;base64,'+json.logicsIcon) : 'favicon.ico'
                    document.head.appendChild(newLink);
                };
                xhttp.open("GET", "exec?action=System.getGUIPreferences%5B%5D&return=System.GUIPreferences%5B%5D", true);
                xhttp.send();
            }
        </script>

        <div id="loadingWrapper">
            <div id="loading" align="center">
                <div class="loadingIndicator">
                    <img id="loadingGif" src="images/loading.gif" width="16" height="16"/>
                    lsFusion<br/>
                    <span id="loadingMsg"><%= ServerMessages.getString(request, "loading") %></span>
                </div>
            </div>
        </div>
        <%-- gwt js src is <module name>/<module name>.nocache.js --%>
        <script type="text/javascript" language="javascript"
                src="main/main.nocache.js"></script>
    </body>
</html>
