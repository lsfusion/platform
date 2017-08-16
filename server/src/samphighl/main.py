from flask import Flask, make_response, request
from sys import argv

from pygments import highlight
from pygments.lexers import get_lexer_by_name
from pygments.formatters.html import HtmlFormatter
from os import path

app = Flask(__name__)

@app.route("/samphighl", methods=['GET', 'POST'])
def index():
    filesPath = argv[1]
    fileName = request.args.get('file', 'Test') + '.lsf'

    with open(path.join(filesPath,fileName)) as file:
        code = file.read()

    formatter = HtmlFormatter(style='colorful',
                              linenos=True,
                              noclasses=True,
                              cssclass='',
                              cssstyles='',
                              prestyles='')
    html = highlight(code, get_lexer_by_name('python', **{}), formatter)

    return make_response(html)

if __name__ == "__main__":
    app.run(host='localhost')
