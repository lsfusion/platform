CodeMirror.defineMode("fusion", function(config, parserConfig) {
    function words(str) {
        var obj = {}, words = str.split(" ");
        for (var i = 0; i < words.length; ++i) {
            obj[words[i]] = true;
        }
        return obj;
    }

    var types = words("INTEGER DOUBLE LONG BOOLEAN DATE DATETIME TEXT STRING ISTRING VARISTRING VARSTRING TIME");
    var keywords = words("ABSTRACT ACTION ADD ADDFORM ADDOBJ ADDSESSIONFORM AFTER " + 
                           "AGGR AGGPROP AND APPLY AS ASONCHANGE ASONCHANGEWYS ASONEDIT ASSIGN ASYNCUPDATE ATTACH " +
                           "AUTO AUTOREFRESH AUTOSET BACKGROUND BCC BEFORE BOTTOM BREAK BY CANCEL CASCADE " +
                           "CASE CC CENTER CHANGE CHANGECLASS CHANGED CHANGEWYS CHECK " + 
                           "CHECKED CLASS CLOSE COLOR COLUMNS CONCAT CONFIRM CONSTRAINT " + 
                           "CONTAINERH CONTAINERV CONTEXTFILTER CSV CUSTOM CUSTOMFILE CYCLES DATA DBF DEFAULT DELETE " +
                           "DELETESESSION DESC DESIGN DIALOG DO DOCKED DOCKEDMODAL DOCX DRAWROOT " +
                           "DROP DROPCHANGED DROPSET ECHO EDIT EDITABLE EDITFORM EDITKEY " +
                           "EDITSESSIONFORM ELSE EMAIL END EQUAL EVAL EVENTID EVENTS EXCELFILE " +
                           "EXCEPTLAST EXCLUSIVE EXEC EXTEND FALSE FILE FILTER FILTERGROUP " + 
                           "FILTERS FIRST FIXED FIXEDCHARWIDTH FOOTER FOR FORCE FOREGROUND " +
                           "FORM FORMS FORMULA FROM FULLSCREEN GOAFTER GRID GROUP HALIGN HEADER " +
                           "HIDE HIDESCROLLBARS HIDETITLE HINTNOUPDATE HINTTABLE HORIZONTAL " + 
                           "HTML IF IMAGE IMAGEFILE IMPORT IMPOSSIBLE IN INCREMENT INDEX " + 
                           "INDEXED INIT INITFILTER INLINE INPUT IS JDBC JOIN LAST LEADING LEFT LENGTH LIMIT " +
                           "LIST LOADFILE LOCAL LOGGABLE MANAGESESSION MAX MAXCHARWIDTH MDB" +
                           "MESSAGE META MIN MINCHARWIDTH MODAL MODULE MULTI NAGGR NAME NAMESPACE " +
                           "NAVIGATOR NEW NEWSESSION NEWTHREAD NO NOHEADER NOT NULL NUMERIC OBJECT " +
                           "OBJECTS OBJVALUE OK ON OPENFILE OPTIMISTICASYNC OR ORDER OVERRIDE PAGESIZE " +
                           "PANEL PARENT PARTITION PDF PDFFILE PERSISTENT POSITION " + 
                           "PREFCHARWIDTH PREV PRINT PRIORITY PROPERTIES PROPERTY " + 
                           "PROPORTION QUERYOK QUERYCLOSE QUICKFILTER READ READONLY READONLYIF RECURSION REGEXP REMOVE " +
                           "REPORTFILES REQUEST REQUIRE RESOLVE RETURN RGB RICHTEXT RIGHT " + 
                           "ROUND RTF SELECTOR SESSION SET SETCHANGED SHORTCUT SHOW SHOWDROP " +
                           "SHOWIF SINGLE SPLITH SPLITV STEP STRETCH STRICT STRUCT SUBJECT " +
                           "SUM TABBED TABLE TEXTHALIGN TEXTVALIGN THEN TIME TO TODRAW " +
                           "TOOLBAR TOP TRAILING TREE TRUE UNGROUP UPDATE VALIGN " +
                           "VERTICAL WHEN WHERE WHILE WINDOW WORDFILE WRITE XLS XLSX XML XOR YES");

    var isOperatorChar = /[+\-*&%=<>!?|@#]/;

    return {
        token: function(stream, state) {
            if (stream.eatSpace()) {
                return null;
            }

            var ch = stream.next();
            if (ch == "'") {
                var escaped = false, next;
                while ((next = stream.next()) != null) {
                    if (next == ch && !escaped) {
                        return "string";
                    }
                    escaped = !escaped && next == "\\";
                }
                return "string";
            } else if (/[\[\]{}\(\),;\:\.]/.test(ch)) {
                return "bracket";
            } else if (/\d/.test(ch)) {
                stream.match(/^\d*(\.\d+)?/);
                return "number";
            } else if (ch == "/") {
                if (stream.eat("/")) {
                    stream.skipToEnd();
                    return "comment";
                }
                return "operator";
            } else if (ch == "$") {
                stream.eatWhile(/[\d]/);
                return "attribute";
            } else if (isOperatorChar.test(ch)) {
                stream.eatWhile(isOperatorChar);
                return "operator";
            } else {
                stream.eatWhile(/[\w]/);
                var word = stream.current();
                var istype = types.propertyIsEnumerable(word) && types[word];

                if (istype) {
                    return "type";
                }
                var iskeyword = keywords.propertyIsEnumerable(word) && keywords[word];
                return iskeyword ? "keyword" : "variable";
            }
        }
    };
});
