CodeMirror.defineMode("fusion", function(config, parserConfig) {
    function words(str) {
        var obj = {}, words = str.split(" ");
        for (var i = 0; i < words.length; ++i) {
            obj[words[i]] = true;
        }
        return obj;
    }

    var types = words("INTEGER DOUBLE LONG BOOLEAN DATE DATETIME TEXT STRING ISTRING TIME");
    var keywords = words("ABSTRACT ACTION ADD ADDFORM ADDOBJ ADDSESSIONFORM AFTER " + 
                           "ALL AND APPLY AS ASC ASSIGN ATTACH AUTOAPPLY AUTOSET BCC " + 
                           "BEFORE BOTTOM BREAK BY CASCADE CASE CC CENTER CHANGE CHECK " + 
                           "CHECKED CLASS COLUMNS CONCAT CONFIRM CONSTRAINT CUSTOM CUSTOMFILE " +
                           "CYCLES DATA DEFAULT DESC DESIGN DIALOG DO DOCKING DOCX " + 
                           "DRAWROOT ECHO EDIT EDITABLE EDITFORM EDITKEY EDITSESSIONFORM " + 
                           "ELSE EMAIL END EQUAL EXCELFILE EXCEPTLAST EXCLUSIVE EXEC FALSE FILTER " +
                           "FILTERGROUP FILTERS FIXED FIXEDCHARWIDTH FOOTER FOR FORCE " +
                           "FORM FORMULA FROM FULLSCREEN GRID GROUP HALIGN HEADER " + 
                           "HIDE HIDESCROLLBARS HIDETITLE HIGHLIGHTIF HINT HORIZONTAL " + 
                           "HTML IF IMAGE IMAGEFILE IMPORT IMPOSSIBLE IN INCREMENT INDEX INDEXED " +
                           "INIT INLINE INTERSECT IS LEFT LENGTH LIST LOGGABLE MAX " + 
                           "MAXCHARWIDTH MESSAGE META MIN MINCHARWIDTH MODAL MODULE " + 
                           "NAME NAVIGATOR NEW NEWSESSION NO NOT NOTHING NULL OBJECT " + 
                           "OBJECTS OBJVALUE OLD ON OR ORDER OVERRIDE PAGESIZE PANEL " + 
                           "PARENT PARTITION PDF PDFFILE PERSISTENT POSITION PREFCHARWIDTH " +
                           "PRINT PROPERTIES PROPERTY READONLY READONLYIF RECURSION " + 
                           "REGEXP REMOVE RESOLVE RETURN RIGHT RIGHTBOTTOM RTF SELECTION " + 
                           "SESSION SET SHORTCUT SHOW SHOWIF STATIC STEP SUBJECT SUM " + 
                           "TABLE TEXTHALIGN TEXTVALIGN THE THEN TO TODRAW TOOLBAR " + 
                           "TOP TREE TRUE UNION UNIQUE UPDATE VALIGN VERTICAL WHEN " + 
                           "WHERE WHILE WINDOW WORDFILE XOR");

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
