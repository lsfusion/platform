define("ace/mode/lsf",["require","exports","module","ace/lib/oop","ace/mode/text","ace/mode/text_highlight_rules","ace/worker/worker_client"], function (require, exports, module) {

    var oop = require("../lib/oop");
    var TextMode = require("./text").Mode;
    var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;
    var LSFHighlightRules = function () {
        var keywordMapper = this.createKeywordMapper({
            "keyword.module": "MODULE|REQUIRE|PRIORITY|NAMESPACE",
            "keyword.class": "CLASS|ABSTRACT|NATIVE|COMPLEX|EXTEND",
            "keyword.group": "GROUP|EXTID",
            "keyword.dialog.form.decl": "LIST|OBJECT",
            "keyword.edit.form.decl": "EDIT",
            "keyword.report.files.decl": "REPORTFILES",
            "keyword.report.path": "TOP",
            "keyword.report.decl": "REPORT",
            "keyword.form.extid.decl": "FORMEXTID",
            "keyword.form.decl": "FORM|IMAGE|AUTOREFRESH|LOCALASYNC",
            "keyword.form.group.obj.list": "OBJECTS",
            "keyword.form.tree.group.obj.list": "TREE",
            "keyword.form.tree.group.obj": "PARENT",
            "keyword.group.obj.class.view.type": "PANEL|TOOLBAR|GRID",
            "keyword.property.custom.view": "CUSTOM|CHANGE",
            "keyword.list.view.type": "PIVOT|DEFAULT|NODEFAULT|MAP|CALENDAR",
            "keyword.property.group.type": "SUM|MAX|MIN",
            "keyword.property.last.aggr": "LAST|DESC",
            "keyword.property.formula": "FORMULA",
            "keyword.form.group.object.page.size": "PAGESIZE",
            "keyword.form.group.object.relative.position": "AFTER|BEFORE|FIRST",
            "keyword.form.group.object.background": "BACKGROUND",
            "keyword.form.group.object.foreground": "FOREGROUND",
            "keyword.form.group.object.update": "PREV",
            "keyword.form.group.object.group": "IN",
            "keyword.form.ext.key": "EXTKEY",
            "keyword.form.sub.report": "SUBREPORT",
            "keyword.form.object.decl": "ON",
            "keyword.form.properties.list": "PROPERTIES",
            "keyword.form.property.options.list": "SELECTOR|HINTNOUPDATE|HINTTABLE|OPTIMISTICASYNC|COLUMNS|SHOWIF|READONLYIF|" +
                "HEADER|FOOTER|DRAW|QUICKFILTER|CONTEXTMENU|KEYPRESS|EVENTID|ATTR|FILTER|COLUMN|ROW|MEASURE",
            "keyword.form.predefined.usage": "NEW|NEWEDIT|VALUE|DELETE",
            "keyword.form.predefined.or.action.usage": "ACTION",
            "keyword.form.filters.list": "FILTERS",
            "keyword.form.events.list": "EVENTS",
            "keyword.form.event.decl": "OK|APPLY|CLOSE|INIT|CANCEL|DROP|QUERYCLOSE",
            "keyword.filter.group.decl": "FILTERGROUP",
            "keyword.user.filters.decl": "USERFILTERS",
            "keyword.form.order.by.list": "ORDERS",
            "keyword.property.draw.order": "ORDER",
            "keyword.form.pivot.options.decl": "ROWS|MEASURES",
            "keyword.pivot.options": "SETTINGS|NOSETTINGS|CONFIG",
            "keyword.property.expr": "IF|OR|XOR|AND|NOT|LIKE|IS|AS",
            "keyword.join.property.definition": "JOIN|AGGR|WHERE",
            "keyword.group.property.definition": "BY",
            "keyword.groupping.type": "NAGGR|EQUAL",
            "keyword.groupping.type.order": "CONCAT",
            "keyword.partition.property.definition": "PARTITION|UNGROUP|PROPORTION|STRICT|ROUND|LIMIT|WINDOW|EXCEPTLAST",
            "keyword.data.property.definition": "DATA|LOCAL",
            "keyword.nested.local.modifier": "NESTED|MANAGESESSION|NOMANAGESESSION",
            "keyword.abstract.property.definition": "CASE|MULTI|FULL",
            "keyword.override.property.definition": "OVERRIDE|EXCLUSIVE",
            "keyword.if.else.property.definition": "THEN|ELSE",
            "keyword.case.branch.body": "WHEN",
            "keyword.recursive.property.definition": "RECURSION|RECURSION|CYCLES|YES|NO|IMPOSSIBLE",
            "keyword.struct.creation.property.definition": "STRUCT",
            "keyword.session.property.definition": "CHANGED|SET|DROPPED|SETCHANGED|DROPCHANGED|SETDROPPED",
            "keyword.active.tab.property.definition": "ACTIVE|TAB",
            "keyword.formula.property.syntax.type": "PG|MS",
            "keyword.group.object.property.definition": "VIEW",
            "keyword.reflection.property.definition": "REFLECTION",
            "keyword.reflection.property.type": "CANONICALNAME",
            "keyword.read.action.definition.body": "READ|CLIENT|DIALOG|TO",
            "keyword.write.action.definition.body": "WRITE|APPEND",
            "keyword.import.action.definition.body": "IMPORT|FROM|FIELDS",
            "keyword.export.action.definition.body": "EXPORT",
            "keyword.new.thread.action.definition.body": "NEWTHREAD|CONNECTION|SCHEDULE|PERIOD|DELAY",
            "keyword.new.executor.action.definition.body": "NEWEXECUTOR|THREADS",
            "keyword.new.session.action.definition.body": "NEWSESSION|NEWSQL|NESTEDSESSION|SINGLE",
            "keyword.import.source.format": "CSV|CHARSET|DBF|MEMO|XLS|SHEET|ALL|JSON|ROOT|XML|TABLE",
            "keyword.persistent.setting": "MATERIALIZED",
            "keyword.complex.setting": "NOCOMPLEX",
            "keyword.preread.setting": "PREREAD",
            "keyword.hint.setting": "HINT|NOHINT",
            "keyword.loggable.setting": "LOGGABLE",
            "keyword.not.null.setting": "NONULL",
            "keyword.shortcut.setting": "ASON",
            "keyword.flex.char.width.setting": "CHARWIDTH|FLEX|NOFLEX",
            "keyword.default.compare.setting": "DEFAULTCOMPARE",
            "keyword.change.key.setting": "CHANGEKEY|SHOW|HIDE",
            "keyword.change.mouse.setting": "CHANGEMOUSE",
            "keyword.autoset.setting": "AUTOSET",
            "keyword.confirm.setting": "CONFIRM",
            "keyword.regexp.setting": "REGEXP",
            "keyword.echo.symbol.setting": "ECHO",
            "keyword.index.setting": "INDEXED|MATCH",
            "keyword.form.event.type": "CHANGEWYS|GROUPCHANGE",
            "keyword.sticky.option": "STICKY|NOSTICKY",
            "keyword.form.action.definition.body": "READONLY|CHECK",
            "keyword.form.session.scope.clause": "THISSESSION",
            "keyword.no.cancel.clause": "NOCANCEL",
            "keyword.do.input.body": "DO",
            "keyword.sync.type.literal": "WAIT|NOWAIT",
            "keyword.window.type.literal": "FLOAT|DOCKED",
            "keyword.print.action.definition.body": "PRINT|PASSWORD|XLSX|PDF|DOC|DOCX|RTF|HTML|MESSAGE|PREVIEW|NOPREVIEW",
            "keyword.export.source.format": "TAG",
            "keyword.has.header.option": "NOHEADER",
            "keyword.no.escape.option": "NOESCAPE|ESCAPE",
            "keyword.form.action.props": "INPUT|NOCONSTRAINTFILTER|NOCHANGE|CONSTRAINTFILTER",
            "keyword.internal.action.definition.body": "INTERNAL",
            "keyword.external.action.definition.body": "EXTERNAL|PARAMS",
            "keyword.external.format": "SQL|EXEC|TCP|UDP|HTTP|BODYURL|BODYPARAMNAMES|BODYPARAMHEADERS|HEADERS|COOKIES|HEADERSTO|COOKIESTO|LSF|EVAL|JAVA",
            "keyword.external.http.method": "GET|POST|PUT",
            "keyword.email.action.definition.body": "EMAIL|SUBJECT|BODY|ATTACH|NAME",
            "keyword.confirm.action.definition.body": "ASK|YESNO",
            "keyword.async.update.action.definition.body": "ASYNCUPDATE",
            "keyword.seek.object.action.definition.body": "SEEK",
            "keyword.expand.group.object.action.definition.body": "EXPAND|DOWN|UP",
            "keyword.collapse.group.object.action.definition.body": "COLLAPSE",
            "keyword.change.class.action.definition.body": "CHANGECLASS",
            "keyword.drill.down.action.definition.body": "DRILLDOWN",
            "keyword.request.action.definition.body": "REQUEST",
            "keyword.activate.action.definition.body": "ACTIVATE|PROPERTY",
            "keyword.recalculate.action.definition.body": "RECALCULATE",
            "keyword.try.action.definition.body": "TRY|CATCH|FINALLY",
            "keyword.case.action.definition.body": "CASE",
            "keyword.apply.action.definition.body": "SERIALIZABLE",
            "keyword.for.action.definition.body": "FOR|WHILE",
            "keyword.terminal.flow.action.definition.body": "BREAK|RETURN",
            "keyword.constraint.statement": "CONSTRAINT|CHECKED",
            "keyword.follows.clause": "RESOLVE|LEFT|RIGHT",
            "keyword.global.event.statement": "SHOWDEP",
            "keyword.base.event": "GLOBAL|FORMS|GOAFTER",
            "keyword.inline.event": "NOINLINE|INLINE",
            "keyword.index.statement": "INDEX",
            "windowkeyword..type": "MENU",
            "keyword.window.options": "HIDETITLE|DRAWROOT|HIDESCROLLBARS|HALIGN|VALIGN|TEXTHALIGN|TEXTVALIGN",
            "keyword.border.position": "BOTTOM",
            "keyword.dock.position": "POSITION",
            "keyword.orientation": "VERTICAL|HORIZONTAL",
            "keyword.navigator.statement": "NAVIGATOR",
            "keyword.move.navigator.element.statement": "MOVE",
            "keyword.navigator.element.description": "FOLDER",
            "keyword.design.header": "DESIGN",
            "keyword.remove.component.statement": "REMOVE",
            "keyword.component.single.selector.type": "BOX|TOOLBARBOX|TOOLBARLEFT|TOOLBARRIGHT",
            "keyword.group.object.tree.component.selector.type": "TOOLBARSYSTEM|FILTERGROUPS|USERFILTER|GRIDBOX|CLASSCHOOSER",
            "keyword.meta.code.decl.statement": "META|END",
            "keyword.color.literal": "RGB",
            "keyword.container.type.literal": "CONTAINERV|CONTAINERH|TABBED|SPLITH|SPLITV|SCROLL",
            "keyword.flex.alignment.literal": "START|CENTER|STRETCH",
            "keyword.property.edit.type.literal": "CHANGEABLE",
            "keyword.email.recipient.type.literal": "CC|BCC",
            "keyword.primitive.type": "INTEGER|DOUBLE|LONG|BOOLEAN|TBOOLEAN|DATE|DATETIME|ZDATETIME|YEAR|TEXT|RICHTEXT|HTMLTEXT|TIME|WORDFILE|IMAGEFILE|" +
                "PDFFILE|DBFFILE|RAWFILE|FILE|EXCELFILE|TEXTFILE|CSVFILE|HTMLFILE|JSONFILE|XMLFILE|TABLEFILE|NAMEDFILE|WORDLINK|IMAGELINK|" +
                "PDFLINK|DBFLINK|RAWLINK|LINK|EXCELLINK|TEXTLINK|CSVLINK|HTMLLINK|JSONLINK|XMLLINK|TABLELINK|BPSTRING" +
                "|BPISTRING|STRING|ISTRING|NUMERIC|COLOR|INTERVAL",
            "keyword.logical.literal": "TRUE|FALSE",
            "keyword.t.logical.literal": "TTRUE|TFALSE",
            "keyword.null.literal": "NULL",

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
            