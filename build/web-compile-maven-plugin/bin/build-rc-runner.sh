#!/bin/bash
# (Re)build the self-contained React Compiler runner (babel core + babel-plugin-react-compiler bundled into one
# minified .cjs) into the plugin's resources (src/main/resources/rc/rc-runner.cjs, COMMITTED — ships inside the
# plugin jar, extracted by the mojo at build time, so consumers need no local runner). Requires node+npm; run only
# when bumping the pinned versions below.
set -euo pipefail
DIR=$(cd "$(dirname "$0")/../src/main/resources/rc" && pwd)
WORK=$(mktemp -d)
cd "$WORK"
npm init -y >/dev/null
npm install --no-audit --no-fund @babel/core@7.29.7 babel-plugin-react-compiler@1.0.0 esbuild@0.25.10 >/dev/null
cat > runner-src.js <<'SRC'
const babel = require('@babel/core');
const compiler = require('babel-plugin-react-compiler');
const fs = require('fs');
const [,, inFile, outFile, target] = process.argv;

// auto-memo: wrap every module-level function component (capitalized, contains JSX) in React.memo.
// function declarations get a live-binding reassignment appended (module bindings are mutable, exports update);
// arrow/function-expression consts wrap the initializer. Skips components already wrapped in memo.
const autoMemo = ({ types: t }) => {
  const containsJSX = (path) => { let found = false; path.traverse({ JSXElement() { found = true; }, JSXFragment() { found = true; } }); return found; };
  // safety gate: only components the compiler actually COMPILED (body calls the _c(N) memo-cache) get auto-memo —
  // a compiler-SKIPPED (rules-violating) component may rely on rendering with its parent; memo there could change behavior
  const isCompiled = (path) => { let found = false; path.traverse({ CallExpression(p) { const c = p.node.callee; if (c.type === 'Identifier' && /^_c\d*$/.test(c.name)) found = true; } }); return found; };
  const isComp = (name) => name && /^[A-Z]/.test(name);
  const memoCall = (state, arg) => t.callExpression(state.memoId, [arg]);
  return {
    pre() { this.wrapped = []; },
    visitor: {
      Program: { exit(path, state) {
        if (!state.wrapped.length) return;
        const memoId = path.scope.generateUidIdentifier('rcMemo');
        path.unshiftContainer('body', t.importDeclaration([t.importSpecifier(memoId, t.identifier('memo'))], t.stringLiteral('react')));
        for (const w of state.wrapped) w(memoId);
      } },
      FunctionDeclaration(path, state) {
        if (path.parent.type !== 'Program' && !(path.parent.type === 'ExportNamedDeclaration')) return;
        const name = path.node.id && path.node.id.name;
        if (!isComp(name) || !containsJSX(path) || !isCompiled(path)) return;
        state.wrapped = state.wrapped || [];
        state.wrapped.push((memoId) => {
          const assign = t.expressionStatement(t.assignmentExpression('=', t.identifier(name), t.callExpression(memoId, [t.identifier(name)])));
          (path.parent.type === 'ExportNamedDeclaration' ? path.parentPath : path).insertAfter(assign);
        });
      },
      ExportDefaultDeclaration(path, state) {
        const decl = path.get('declaration');
        if (!(decl.isFunctionDeclaration() || decl.isFunctionExpression() || decl.isArrowFunctionExpression())) return;
        const name = decl.node.id && decl.node.id.name;
        if ((name && !isComp(name)) || !containsJSX(decl) || !isCompiled(decl)) return;
        state.wrapped = state.wrapped || [];
        state.wrapped.push((memoId) => {
          const node = decl.node.type === 'FunctionDeclaration' ? t.functionExpression(decl.node.id, decl.node.params, decl.node.body, decl.node.generator, decl.node.async) : decl.node;
          decl.replaceWith(t.callExpression(memoId, [node]));
        });
      },
      VariableDeclarator(path, state) {
        const name = path.node.id.type === 'Identifier' && path.node.id.name;
        const init = path.get('init');
        if (!isComp(name) || !init.node) return;
        if (!(init.isArrowFunctionExpression() || init.isFunctionExpression()) || !containsJSX(init) || !isCompiled(init)) return;
        if (path.parentPath.parent.type !== 'Program' && path.parentPath.parent.type !== 'ExportNamedDeclaration') return;
        state.wrapped = state.wrapped || [];
        const node = init.node;
        state.wrapped.push((memoId) => { init.replaceWith(t.callExpression(memoId, [node])); });
      }
    }
  };
};

const result = babel.transformSync(fs.readFileSync(inFile, 'utf8'), {
  filename: inFile,
  parserOpts: { plugins: [/\.ts$/.test(inFile) ? null : 'jsx', /\.tsx?$/.test(inFile) ? 'typescript' : null].filter(Boolean) }, // plain .ts: jsx plugin breaks generic arrows (<T>(x) => ...)
  plugins: [[compiler, { target: target || '18', panicThreshold: 'none' }], autoMemo],
  babelrc: false, configFile: false,
});
fs.writeFileSync(outFile, result.code);
SRC
./node_modules/.bin/esbuild runner-src.js --bundle --platform=node --format=cjs \
  --external:@babel/preset-typescript --external:@babel/preset-react --external:@babel/preset-env \
  --minify --outfile="$DIR/rc-runner.cjs" --log-level=error
echo "built: $DIR/rc-runner.cjs ($(wc -c < "$DIR/rc-runner.cjs") bytes)"
