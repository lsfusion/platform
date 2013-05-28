        <!--CSS for loading message at application Startup-->
        <style type="text/css">
            body {
                overflow: hidden
            }

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

            #loadingMsg {
                font: normal 13px arial, tahoma, sans-serif;
            }
        </style>

        <!--add loading indicator while the app is being loaded-->
        <div id="loadingWrapper">
            <div id="loading">
                <div class="loadingIndicator">
                    <img src="images/loading.gif" width="16" height="16"
                         style="margin-right:8px;float:left;vertical-align:top;"/>Skolkovo<br/>
                    <span id="loadingMsg">Loading styles and images...</span></div>
            </div>
        </div>

        <script type="text/javascript">document.getElementById('loadingMsg').innerHTML = 'Loading Core API...';</script>

        <script> var isomorphicDir = "smartgwt/sc/"; </script>

        <!--include the SC Core API-->
        <script src="smartgwt/sc/modules/ISC_Core.js"></script>

        <!--include SmartClient -->
        <script type="text/javascript">document.getElementById('loadingMsg').innerHTML = 'Loading UI Components...';</script>
        <script src='smartgwt/sc/modules/ISC_Foundation.js'></script>
        <script src='smartgwt/sc/modules/ISC_Containers.js'></script>
        <script src='smartgwt/sc/modules/ISC_Grids.js'></script>
        <script src='smartgwt/sc/modules/ISC_Forms.js'></script>
        <script src='smartgwt/sc/modules/ISC_RichTextEditor.js'></script>
        <script src='smartgwt/sc/modules/ISC_Calendar.js'></script>
        <script src='smartgwt/sc/modules/ISC_History.js'></script>
        <script src='smartgwt/sc/modules/ISC_PluginBridges.js'></script>

        <script type="text/javascript">document.getElementById('loadingMsg').innerHTML = 'Loading Data API...';</script>
        <script src='smartgwt/sc/modules/ISC_DataBinding.js'></script>

        <!--load skin-->
        <script type="text/javascript">document.getElementById('loadingMsg').innerHTML = 'Loading skin...';</script>
        <script src='smartgwt/sc/skins/Enterprise/load_skin.js?isc_version=8.1.js'></script>

        <!--include the application JS-->
        <script type="text/javascript">document.getElementById('loadingMsg').innerHTML = 'Loading Application<br>Please wait...';</script>
        <script type="text/javascript" language="javascript" src="smartgwt/smartgwt.nocache.js"></script>
