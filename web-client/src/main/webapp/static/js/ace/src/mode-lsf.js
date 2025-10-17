define("ace/mode/lsf",["require","exports","module","ace/lib/oop","ace/mode/text","ace/mode/text_highlight_rules","ace/worker/worker_client"], function (require, exports, module) {

    var oop = require("../lib/oop");
    var TextMode = require("./text").Mode;
    var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;
    var LSFHighlightRules = function () {
        var keywordMapper = this.createKeywordMapper({
            "keyword": "BOOLEAN|TBOOLEAN|BPISTRING|BPSTRING|COLOR|CSVFILE|CSVLINK|DATE|DATETIME|INTERVAL|DOUBLE|EXCELFILE|EXCELLINK|FILE|HTMLFILE|HTMLLINK|IMAGEFILE|IMAGELINK|INTEGER|ISTRING|JSONFILE|JSONLINK|LINK|LONG|NAMEDFILE|NUMERIC|PDFFILE|PDFLINK|DBFFILE|DBFLINK|RAWFILE|RAWLINK|RICHTEXT|STRING|TABLEFILE|TABLELINK|TEXT|TEXTFILE|TEXTLINK|TIME|WORDFILE|WORDLINK|XMLFILE|XMLLINK|YEAR|ABSTRACT|ACTION|ACTIONS|ACTIVATE|ACTIVE|AFTER|AGGR|ALL|AND|APPEND|APPLY|AS|ASK|ASON|ASYNCUPDATE|ATTACH|ATTR|AUTO|AUTOREFRESH|AUTOSET|BACKGROUND|BCC|BEFORE|BODY|BODYPARAMHEADERS|BODYPARAMNAMES|BODYURL|BOTTOM|BOX|BREAK|BY|CALENDAR|CANCEL|CANONICALNAME|CASE|CATCH|CC|CENTER|CHANGE|CHANGEABLE|CHANGECLASS|CHANGED|CHANGEKEY|CHANGEMOUSE|CHANGEWYS|CHARSET|CHARWIDTH|CHECK|CHECKED|CLASS|CLASSCHOOSER|CLIENT|CLOSE|COLLAPSE|COLUMN|COLUMNS|COMPLEX|CONCAT|CONFIG|CONFIRM|CONNECTION|CONSTRAINT|CONSTRAINTFILTER|CONTAINER|CONTEXTMENU|CONTINUE|COOKIES|COOKIESTO|CSV|CUSTOM|CYCLES|DATA|DB|DBF|DEFAULT|DEFAULTCOMPARE|DELAY|DELETE|DESC|DESIGN|DIALOG|DISABLE|DISABLEIF|DO|DOC|DOCKED|DOCX|DOWN|DRAW|DRILLDOWN|DROP|DROPCHANGED|DROPPED|ECHO|EDIT|ELSE|EMAIL|EMBEDDED|END|EQUAL|ESCAPE|EVAL|EVENTID|EVENTS|EXCLUSIVE|EXEC|EXPAND|EXPORT|EXTEND|EXTERNAL|EXTID|EXTKEY|EXTNULL|FALSE|FIELDS|FILTER|FILTERBOX|FILTERCONTROLS|FILTERGROUP|FILTERGROUPS|FILTER|FILTERS|FINALLY|FIRST|FIXED|FLEX|FLOAT|FOCUSED|FOLDER|FOOTER|FOR|FOREGROUND|FORM|FORMEXTID|FORMS|FORMULA|FROM|FULL|GET|GLOBAL|GOAFTER|GRID|GROUP|GROUPCHANGE|HALIGN|HEADER|HEADERS|HEADERSTO|HIDE|HIDESCROLLBARS|HIDETITLE|HINT|HINTNOUPDATE|HINTTABLE|HORIZONTAL|HOVER|HTML|HTTP|IF|IMAGE|IMPORT|IMPOSSIBLE|IN|INDEX|INDEXED|INIT|INLINE|INPUT|INTERNAL|IS|JAVA|JOIN|JSON|JSONTEXT|KEYPRESS|LAST|LEFT|LIKE|LIMIT|LIST|LOCAL|LOCALASYNC|LOG|LOGGABLE|LSF|MANAGESESSION|MAP|MATERIALIZED|MATCH|MAX|MEASURE|MEASURES|MEMO|MENU|MESSAGE|META|MIN|MODULE|MOVE|MS|MULTI|NAGGR|NAME|NAMESPACE|NATIVE|NAVIGATOR|NESTED|NESTEDSESSION|NEW|NEWCONNECTION|NEWEDIT|NEWEXECUTOR|NEWSESSION|NEWSQL|NEWTHREAD|NO|NOCANCEL|NOCHANGE|NOCOMPLEX|NOCONSTRAINTFILTER|NODEFAULT|NOENCODE|NOEXTID|NOESCAPE|NOFLEX|NOHEADER|NOHINT|NOINLINE|NOMANAGESESSION|NONULL|NOPREVIEW|NOREPLACE|NOSETTINGS|NOSTICKY|NOT|NOWAIT|NULL|OBJECT|OBJECTS|OK|ON|OPTIMISTICASYNC|OPTIONS|OR|ORDER|ORDERS|OVERRIDE|PAGESIZE|PANEL|PARAMS|PARENT|PARTITION|PASSWORD|PDF|PERIOD|PG|POPUP|PIVOT|POSITION|POST|PREREAD|PREV|PREVIEW|PRINT|PRIORITY|PROPERTIES|PROPERTY|PROPORTION|PUT|QUERYCLOSE|QUERYOK|QUICKFILTER|READ|READONLY|READONLYIF|RECALCULATE|RECURSION|REFLECTION|REGEXP|REMOVE|REPLACE|REPORT|REPORTFILES|REQUEST|REQUIRE|RESOLVE|RETURN|RGB|RIGHT|ROOT|ROUND|ROW|ROWS|RTF|SCHEDULE|SEEK|SELECTED|SELECTOR|SERIALIZABLE|SERVER|SET|SETCHANGED|SETDROPPED|SETTINGS|SHEET|SHOW|SHOWDEP|SHOWIF|SINGLE|SQL|START|STEP|STICKY|STRETCH|STRICT|STRONG|STRUCT|SUBJECT|SUBREPORT|SUCCESS|SUM|TAB|TABLE|TAG|TCP|TEXTHALIGN|TEXTVALIGN|TFALSE|THEN|THREADS|TO|TOOLBAR|TOOLBARBOX|TOOLBARLEFT|TOOLBARRIGHT|TOOLBARSYSTEM|TOP|TREE|TRUE|TRY|TTRUE|UDP|UNGROUP|UP|USERFILTERS|VALIGN|VALUE|VERTICAL|VIEW|WAIT|WARN|WEAK|WHEN|WHERE|WHILE|WINDOW|WITHIN|WRITE|XLS|XLSX|XML|XOR|YES|YESNO|ZDATETIME"
        }, "identifier");
        this.$rules = {
            "start": [
                {
                    token: "comment",
                    regex: "\\/\\/.*$"
                },
                {
                    token : "string",
                    regex : "['](?:(?:\\\\.)|(?:[^'\\\\]))*?[']"
                },
                {
                    token: keywordMapper,
                    regex: "[a-zA-Z_$][a-zA-Z0-9_$]*\\b"
                }
            ]
        };
    };
    oop.inherits(LSFHighlightRules, TextHighlightRules);

    var Mode = function () {
        this.HighlightRules = LSFHighlightRules;
    };
    oop.inherits(Mode, TextMode);

    (function () {
        this.lineCommentStart = "//"; // this is need to "ctrl + /" hotkey worked correctly
        this.$id = "ace/mode/lsf";

        var WorkerClient = require("../worker/worker_client").WorkerClient;
        this.createWorker = function (session) {

            var worker = new WorkerClient(["ace"], "ace/mode/lsf_worker", "LSFWorker");
            worker.call("setOptions", [{lsfWorkerType: session.getOption('lsfWorkerType')}]);

            worker.attachToDocument(session.getDocument());

            worker.on("errors", function (e) {
                session.setAnnotations(e.data);
            });

            worker.on("annotate", function (e) {
                session.setAnnotations(e.data);
            });

            worker.on("terminate", function () {
                session.clearAnnotations();
            });

            return worker;

        };

    }).call(Mode.prototype);

    exports.Mode = Mode;
});                (function() {
                    window.require(["ace/mode/lsf"], function(m) {
                        if (typeof module == "object" && typeof exports == "object" && module) {
                            module.exports = m;
                        }
                    });
                })();
            