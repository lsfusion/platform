<%@ page import="lsfusion.base.ServerMessages" %>
<%@ page isELIgnored="false" %>

<!DOCTYPE html>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        
        <title>${title}</title>
        <link rel="shortcut icon" href="{$logicsIcon}" />
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
    <body>
        <script language="JavaScript">
            var pageSetup = {
                webAppRoot: "<%= request.getContextPath() + "/" %>"
            };
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
