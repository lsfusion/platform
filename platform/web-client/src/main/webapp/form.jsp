<!DOCTYPE html>

<%@ page import="platform.gwt.base.server.ServerUtils" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        <meta http-equiv="cache-control" content="no-store, no-cache, must-revalidate"/>
        <meta http-equiv="Pragma" content="no-store, no-cache"/>
        <meta http-equiv="Expires" content="0"/>
        <meta name="gwt:property" content="locale=<%=ServerUtils.getLocaleLanguage()%>">

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
                margin-right:8px;
                float:left;
                vertical-align:top;
            }

            #loadingMsg {
                font: normal 13px arial, tahoma, sans-serif;
            }
        </style>
    </head>
    <body>
        <div id="loadingWrapper">
            <div id="loading">
                <div class="loadingIndicator">
                    <img id="loadingGif" src="images/loading.gif" width="16" height="16"/>
                    lsFusion<br/>
                    <span id="loadingMsg">Загрузка...</span>
                </div>
            </div>
        </div>
        <script type="text/javascript" language="javascript"
                src="platform.gwt.form.Form/platform.gwt.form.Form.nocache.js"></script>
    </body>
</html>
