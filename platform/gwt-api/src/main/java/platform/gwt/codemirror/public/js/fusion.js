CodeMirror.defineMode("fusion", function(config, parserConfig) {
    function words(str) {
        var obj = {}, words = str.split(" ");
        for (var i = 0; i < words.length; ++i) {
            obj[words[i]] = true;
        }
        return obj;
    }

    var types = words("INTEGER DOUBLE LONG BOOLEAN DATE STRING ISTRING");
    var keywords = words("ABSTRACT ACTION ADD ADDOBJ AFTER ALL AND APPLY AS ASC BEFORE " + 
                           "BOTTOM BY CASCADE CHECKED CLASS COLUMNS CONSTRAINT DATA " + 
                           "DEFAULT DESC DESIGN EDITABLE EXCEPTLAST EXCLUSIVE FALSE " + 
                           "FILTER FILTERGROUP FILTERS FIXED FOOTER FORM FORMULA FROM " + 
                           "GRID GROUP HEADER HIDE HIGHLIGHTIF HINT IF IMPORT IN INCREMENT " + 
                           "INIT INTERSECT IS LEFT MAX MODULE MSG NO NOT NOTHING NULL " + 
                           "OBJECTS OBJVALUE ORDER OVERRIDE PANEL PARENT PARTITION " + 
                           "PERSISTENT POSITION PROPERTIES PROPERTY READONLY REMOVE " + 
                           "RESOLVE RIGHT RIGHTBOTTOM SELECTION SHOWIF STATIC SUM " + 
                           "TABLE THE TO TREE TRUE UNION UPDATE WINDOW XOR");

    var isOperatorChar = /[+\-*&%=<>!?|]/;

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
