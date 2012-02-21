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
                           "ALL AND APPLY AS ASC ASSIGN AUTOSET BEFORE BOTTOM BY CASCADE " + 
                           "CASE CENTER CHANGE CHECKED CLASS COLUMNS CONCAT CONFIRM " + 
                           "CONSTRAINT CUSTOM DATA DEFAULT DESC DESIGN DIALOG DO DRAWROOT " +
                           "EDIT EDITABLE EDITFORM EDITKEY EDITSESSIONFORM ELSE END " + 
                           "EXCEPTLAST EXCLUSIVE EXEC FALSE FILTER FILTERGROUP FILTERS " + 
                           "FIXED FIXEDCHARWIDTH FOOTER FOR FORCE FORM FORMULA FROM " + 
                           "GRID GROUP HALIGN HEADER HIDE HIDESCROLLBARS HIDETITLE " + 
                           "HIGHLIGHTIF HINT HORIZONTAL IF IMAGE IMPORT IN INCREMENT " + 
                           "INIT INTERSECT IS LEFT LIST MAX MAXCHARWIDTH MESSAGE META " + 
                           "MIN MINCHARWIDTH MODAL MODULE MSG NAVIGATOR NEW NEWSESSION " + 
                           "NO NOAPPLY NOT NOTHING NULL OBJECT OBJECTS OBJVALUE OLD " + 
                           "ON ORDER OVERRIDE PANEL PARENT PARTITION PERSISTENT POSITION " + 
                           "PREFCHARWIDTH PROPERTIES PROPERTY READONLY READONLYIF " + 
                           "REGEXP REMOVE RESOLVE RIGHT RIGHTBOTTOM SELECTION SET " + 
                           "SHORTCUT SHOW SHOWIF STATIC SUM TABLE TEXTHALIGN TEXTVALIGN " + 
                           "THE THEN TO TODRAW TOOLBAR TOP TREE TRUE UNION UNIQUE " + 
                           "UPDATE VALIGN VERTICAL WHEN WHERE WHILE WINDOW XOR");

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
