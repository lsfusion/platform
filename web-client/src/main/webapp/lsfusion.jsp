<!DOCTYPE html>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        <meta http-equiv="cache-control" content="no-store, no-cache, must-revalidate"/>
        <meta http-equiv="Pragma" content="no-store, no-cache"/>
        <meta http-equiv="Expires" content="0"/>

        <link rel="shortcut icon" href="favicon.ico" />
        
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
                    <span id="loadingMsg">Loading...</span>
                </div>
            </div>
        </div>
        <script type="text/javascript" language="javascript"
                src="lsfusion/lsfusion.nocache.js"></script>
    </body>
</html>
