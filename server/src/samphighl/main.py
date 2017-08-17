from flask import Flask, make_response, request
from sys import argv

from pygments import highlight
from pygments.formatters.html import HtmlFormatter
from pygments.lexer import RegexLexer, words
from pygments.token import *
from os import path


class LSFLexer(RegexLexer):
    """
    For `LS Fusion <http://lsfusion.ru/>`_ files.
    .. versionadded:: 1.0
    """
    name = 'LSF'
    aliases = ['lsf']
    filenames = ['*.lsf']
    mimetypes = ['text/lsf']

    tokens = {
        'root': [
            (r"//.*$", Comment.Single),                 # comments
            (r"\'(?:\\'|\\\\|[^\n\r'])*\'", String),    # string literal
            (r"#{2,3}", Name.Decorator),                # lexems concatenate 
            (r"@[a-zA-Z]\w*\b", Name.Decorator),        # metacode usage 
            (r"\b\d{4}_\d\d_\d\d(?:_\d\d:\d\d)?\b", Number.Integer), # DATE, DATETIME
            (r"\b\d\d:\d\d\b", Number.Integer),         # TIME
            (r"\b\d+\.\d*(?:D|d)?\b", Number.Float),    # NUMERIC, DOUBLE 
            (r"\b\d+(?:l|L)?\b", Number.Integer),       # INTEGER, LONG
            (r"#[0-9A-Fa-f]{6}", Number.Integer),       # COLOR        
            (words(('INTEGER', 'DOUBLE', 'LONG', 'BOOLEAN', 'DATE', 'DATETIME', 'TEXT', 'STRING', 'ISTRING', 'VARISTRING', 'VARSTRING', 'TIME', 'RICHTEXT',
                    'ABSTRACT', 'ACTION', 'ACTIVE', 'ACTIVATE', 'ADD', 'ADDFORM', 'NEW', 'AFTER',
                    'AGGR', 'AGGPROP', 'AND', 'APPLY', 'AS', 'ASON', 'ASSIGN', 'ASYNCUPDATE', 'ATTACH',
                    'ATTR', 'AUTO', 'AUTOREFRESH', 'AUTOSET', 'BACKGROUND', 'BCC', 'BEFORE', 'BOTTOM', 'BREAK', 'BY', 'CANCEL', 'CANONICALNAME',
                    'CASE', 'CATCH', 'CC', 'CENTER', 'CHANGE', 'CHANGECLASS', 'CHANGED', 'CHANGEWYS', 'CHARSET', 'CHECK',
                    'CHECKED', 'CLASS', 'CLOSE', 'COLOR', 'COLUMNS', 'COMPLEX', 'CONCAT', 'CONFIRM', 'CONNECTION', 'CONSTRAINT',
                    'CONTAINERH', 'CONTAINERV', 'CONTEXTFILTER', 'CSV', 'CUSTOM', 'CUSTOMFILE', 'CUSTOMLINK', 'CYCLES', 'DATA', 'DBF', 'DEFAULT', 'DEFAULTCOMPARE', 'DELAY', 'DELETE',
                    'DESC', 'DESIGN', 'DIALOG', 'DO', 'DOC', 'DOCKED', 'DOCKEDMODAL', 'DOCX', 'DRAWROOT',
                    'DROP', 'DROPCHANGED', 'DROPSET', 'ECHO', 'EDIT', 'EDITABLE', 'EDITFORM', 'EDITKEY',
                    'ELSE', 'EMAIL', 'END', 'EQUAL', 'EVAL', 'EVENTID', 'EVENTS', 'EXCELFILE', 'EXCELLINK',
                    'EXCEPTLAST', 'EXCLUSIVE', 'EXEC', 'EXPORT', 'EXTEND', 'FALSE', 'FILE', 'FILTER', 'FILTERGROUP',
                    'FILTERS', 'FINALLY', 'FIRST', 'FIXED', 'FIXEDCHARWIDTH', 'FOCUS', 'FOOTER', 'FOR', 'FORCE', 'FOREGROUND',
                    'FORM', 'FORMS', 'FORMULA', 'FROM', 'FULL', 'FULLSCREEN', 'GOAFTER', 'GRID', 'GROUP', 'GROUPCHANGE', 'HALIGN', 'HEADER',
                    'HIDE', 'HIDESCROLLBARS', 'HIDETITLE', 'HINTNOUPDATE', 'HINTTABLE', 'HORIZONTAL',
                    'HTML', 'IF', 'IMAGE', 'IMAGEFILE', 'IMAGELINK', 'IMPORT', 'IMPOSSIBLE', 'IN', 'INCREMENT', 'INDEX',
                    'INDEXED', 'INIT', 'INITFILTER', 'INLINE', 'INPUT', 'IS', 'JDBC', 'JOIN', 'JSON', 'LAST', 'LEADING', 'LEFT', 'LENGTH', 'LIMIT',
                    'LIST', 'LOADFILE', 'LOCAL', 'LOGGABLE', 'MANAGESESSION', 'MAX', 'MAXCHARWIDTH', 'MDB',
                    'MEMO', 'MESSAGE', 'META', 'MIN', 'MINCHARWIDTH', 'MODAL', 'MODULE', 'MOVE', 'MS', 'MULTI', 'NAGGR', 'NAME', 'NAMESPACE',
                    'NAVIGATOR', 'NESTED', 'NEW', 'NEWEXECUTOR', 'NEWSESSION', 'NEWSQL', 'NEWTHREAD', 'NO', 'NOCANCEL', 'NOHEADER', 'NOHINT', 'NOT', 'NOWAIT', 'NULL', 'NUMERIC', 'OBJECT',
                    'OBJECTS', 'OK', 'ON', 'OPEN', 'OPTIMISTICASYNC', 'OR', 'ORDER', 'OVERRIDE', 'PAGESIZE',
                    'PANEL', 'PARENT', 'PARTITION', 'PDF', 'PDFFILE', 'PDFLINK', 'PERIOD', 'MATERIALIZED', 'PG', 'POSITION',
                    'PREFCHARWIDTH', 'PREV', 'PRINT', 'PRIORITY', 'PROPERTIES', 'PROPERTY',
                    'PROPORTION', 'QUERYOK', 'QUERYCLOSE', 'QUICKFILTER', 'READ', 'READONLY', 'READONLYIF', 'RECURSION', 'REFLECTION', 'REGEXP', 'REMOVE',
                    'REPORTFILES', 'REQUEST', 'REQUIRE', 'RESOLVE', 'RETURN', 'RGB', 'RIGHT',
                    'ROUND', 'RTF', 'SAVE', 'SCHEDULE', 'SCROLL', 'SEEK', 'SELECTOR', 'SESSION', 'SET', 'SETCHANGED', 'SHORTCUT', 'SHOW', 'SHOWDROP',
                    'SHOWIF', 'SINGLE', 'SHEET', 'SPLITH', 'SPLITV', 'STEP', 'STRETCH', 'STRICT', 'STRUCT', 'SUBJECT',
                    'SUM', 'TAB', 'TABBED', 'TABLE', 'TEXTHALIGN', 'TEXTVALIGN', 'THEN', 'THREADS', 'TIME', 'TO', 'TODRAW',
                    'TOOLBAR', 'TOP', 'TRAILING', 'TREE', 'TRUE', 'TRY', 'UNGROUP', 'UPDATE', 'VALIGN', 'VALUE',
                    'VERTICAL', 'VIEW', 'WHEN', 'WHERE', 'WHILE', 'WINDOW', 'WORDFILE', 'WORDLINK', 'WRITE', 'XLS', 'XLSX', 'XML', 'XOR', 'YES'), prefix=r'\b', suffix=r'\b'),
             Keyword),
            (r".", Text),
        ]
    }



app = Flask(__name__)

@app.route("/samphighl", methods=['GET', 'POST'])
def index():
    filesPath = argv[1]
    fileName = request.args.get('file', 'Test') + '.lsf'

    with open(path.join(filesPath,fileName)) as file:
        code = file.read()

    formatter = HtmlFormatter(HtmlFormatter(style='tango', linenos='table', noclasses=True))
    html = highlight(code, LSFLexer(), formatter)

    return make_response(html)

if __name__ == "__main__":
    app.run(host='localhost')
