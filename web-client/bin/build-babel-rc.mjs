#!/usr/bin/env node
// (Re)build the lightweight-.jsx transformer bundle (babel-standalone + babel-plugin-react-compiler bundled into one
// minified GraalJS-compatible iife) into the web client's resources (src/main/resources/lsfusion/jsx/babel-rc.min.js,
// COMMITTED — an internal classpath resource of lsfusion.gwt.server.JsxTransformer, never served to the browser).
// Requires node+npm (run: node bin/build-babel-rc.mjs, any OS); run only when bumping the pinned versions below,
// then update babel-rc.PROVENANCE.md.
import {spawnSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', 'src', 'main', 'resources', 'lsfusion', 'jsx');
const LOCK = path.join(DIR, 'babel-rc.package-lock.json'); // full transitive pin (incl. the build-time esbuild), referenced by babel-rc.PROVENANCE.md
const BABEL_STANDALONE = '7.29.7';
const REACT_COMPILER = '1.0.0';
const ESBUILD = '0.25.10';
const WORK = fs.mkdtempSync(path.join(os.tmpdir(), 'babel-rc-'));

// npm is a .cmd shim on Windows, which Node refuses to spawn without a shell (EINVAL, CVE-2024-27980) —
// only there the shell is enabled; every argument below is a plain token, so nothing needs quoting
function run(cmd, args, quietStdout) {
    const r = spawnSync(cmd, args, {cwd: WORK, shell: process.platform === 'win32' && cmd.endsWith('.cmd'),
        stdio: ['ignore', quietStdout ? 'ignore' : 'inherit', 'inherit']});
    if (r.error) throw r.error;
    if (r.status !== 0) throw new Error(cmd + ' ' + args.join(' ') + ' exited with code ' + r.status);
}
const NPM = process.platform === 'win32' ? 'npm.cmd' : 'npm';

// reproducibility: reuse the committed lockfile when present, so a rebuild resolves the exact same transitive
// graph (delete the lockfile when bumping the pinned versions above); the resulting lock is copied back below.
// The work package.json must carry the lockfile's own root name/version/license (recorded from the original
// build's `npm init -y`), or npm rewrites them in the copied-back lockfile on every rebuild.
let pkg = {name: 'babel-rc-bundle', version: '1.0.0', license: 'ISC'};
if (fs.existsSync(LOCK)) {
    fs.copyFileSync(LOCK, path.join(WORK, 'package-lock.json'));
    const lockRoot = JSON.parse(fs.readFileSync(LOCK, 'utf8')).packages[''];
    pkg = {name: lockRoot.name, version: lockRoot.version, license: lockRoot.license};
}
fs.writeFileSync(path.join(WORK, 'package.json'), JSON.stringify(pkg, null, 2) + '\n');
run(NPM, ['install', '--no-audit', '--no-fund',
    '@babel/standalone@' + BABEL_STANDALONE, 'babel-plugin-react-compiler@' + REACT_COMPILER, 'esbuild@' + ESBUILD], true);
fs.copyFileSync(path.join(WORK, 'package-lock.json'), LOCK);

// node builtins pulled in by babel-plugin-react-compiler: stub what is only probed, never really used
const STUBS = {
    'os.js': String.raw`module.exports = {EOL: '\n', platform: () => 'linux', cpus: () => [], hostname: () => '', tmpdir: () => '/tmp', type: () => 'Linux'};
`,
    'tty.js': String.raw`module.exports = {isatty: () => false};
`,
    'util.js': String.raw`module.exports = {
    format: function () { return Array.prototype.join.call(arguments, ' '); },
    inspect: function (x) { return String(x); },
    inherits: function (c, p) { c.super_ = p; Object.setPrototypeOf(c.prototype, p.prototype); },
    deprecate: function (f) { return f; },
    types: {}
};
`,
    'fs.js': String.raw`module.exports = {existsSync: () => false, readFileSync: () => { throw new Error('fs is not available'); }, statSync: () => { throw new Error('fs is not available'); }};
`,
    'path.js': String.raw`module.exports = {
    sep: '/', delimiter: ':',
    join: function () { return Array.prototype.filter.call(arguments, Boolean).join('/'); },
    resolve: function () { return Array.prototype.filter.call(arguments, Boolean).join('/'); },
    basename: p => String(p).split('/').pop(),
    dirname: p => String(p).split('/').slice(0, -1).join('/') || '.',
    extname: p => { const m = /\.[^./]*$/.exec(String(p)); return m ? m[0] : ''; },
    relative: (a, b) => b, isAbsolute: p => String(p).startsWith('/'), normalize: p => p
};
`,
    'buffer.js': String.raw`module.exports = {Buffer: {from: s => s, isBuffer: () => false}};
`,
    'crypto.js': String.raw`// deterministic non-crypto hash is enough: the plugin uses createHash only for cache keys/ids
module.exports = {
    createHash: function () {
        var acc = '';
        return {
            update: function (s) { acc += s; return this; },
            digest: function () {
                var h1 = 0x811c9dc5, h2 = 0x01000193;
                for (var i = 0; i < acc.length; i++) { h1 = ((h1 ^ acc.charCodeAt(i)) * 16777619) >>> 0; h2 = ((h2 + acc.charCodeAt(i)) * 31) >>> 0; }
                return ('00000000' + h1.toString(16)).slice(-8) + ('00000000' + h2.toString(16)).slice(-8);
            }
        };
    }
};
`
};
fs.mkdirSync(path.join(WORK, 'stubs'));
for (const [name, src] of Object.entries(STUBS))
    fs.writeFileSync(path.join(WORK, 'stubs', name), src);

const ENTRY = String.raw`// require.resolve('react-compiler-runtime') probe by the compiler plugin: pretend the module is resolvable
globalThis.__rcResolveShim = function (m) { return m; };
const Babel = require('@babel/standalone');
const compiler = require('babel-plugin-react-compiler');

// auto-memo: wrap every module-level function component (capitalized, contains JSX) in React.memo.
// function declarations get a live-binding reassignment appended (a top-level function declaration in the served
// classic script is a window global, and the assignment updates it); arrow/function-expression consts wrap the
// initializer. Ported from the compiled tier's runner (build/web-compiler-core/bin/build-rc-runner.sh in the
// webrt build), with one adaptation: this tier has no modules, so the memo binding is `+ '`const _rcMemo = React.memo;`' + String.raw`
// (against the platform-provided window.React) instead of an import from 'react'.
const autoMemo = ({ types: t }) => {
  const containsJSX = (path) => { let found = false; path.traverse({ JSXElement() { found = true; }, JSXFragment() { found = true; } }); return found; };
  // safety gate: only components the compiler actually COMPILED (body calls the _c(N) memo-cache) get auto-memo —
  // a compiler-SKIPPED (rules-violating) component may rely on rendering with its parent; memo there could change behavior
  const isCompiled = (path) => { let found = false; path.traverse({ CallExpression(p) { const c = p.node.callee; if (c.type === 'Identifier' && /^_c\d*$/.test(c.name)) found = true; } }); return found; };
  const isComp = (name) => name && /^[A-Z]/.test(name);
  return {
    pre() { this.wrapped = []; },
    visitor: {
      Program: { exit(path, state) {
        if (!state.wrapped.length) return;
        const memoId = path.scope.generateUidIdentifier('rcMemo');
        path.unshiftContainer('body', t.variableDeclaration('const', [t.variableDeclarator(memoId, t.memberExpression(t.identifier('React'), t.identifier('memo')))]));
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

// the compiler emits ` + "`import { c as _c } from 'react-compiler-runtime'`" + String.raw` (target 18; a 19 target would use
// 'react/compiler-runtime') for its memo cache — rewrite ONLY those two sources into consts against the
// window.lsfusion.rcRuntime shim (server/src/main/resources/web/lsfusion-rc-runtime.js), since the served
// classic script has no modules. Any OTHER import is user code and must survive untouched to the
// JsxTransformer preflight, which rejects it with the tier message.
const rewriteRuntimeImports = ({ types: t }) => ({
  visitor: {
    Program: { exit(path) {
      for (const stmt of path.get('body')) {
        if (!stmt.isImportDeclaration()) continue;
        const source = stmt.node.source.value;
        if (source !== 'react-compiler-runtime' && source !== 'react/compiler-runtime') continue;
        // only the exact shape the compiler emits — named specifiers with identifier imported names — is
        // rewritten; any other shape (default, namespace, side-effect-only, string-named) can only be user
        // code, so the import is left untouched for the JsxTransformer preflight to reject with the tier message
        const specifiers = stmt.node.specifiers;
        if (!specifiers.length || !specifiers.every(s => s.type === 'ImportSpecifier' && s.imported.type === 'Identifier')) continue;
        const runtime = t.memberExpression(t.memberExpression(t.identifier('window'), t.identifier('lsfusion')), t.identifier('rcRuntime'));
        stmt.replaceWith(t.variableDeclaration('const', specifiers.map(s =>
          t.variableDeclarator(t.identifier(s.local.name), t.memberExpression(runtime, t.identifier(s.imported.name))))));
      }
    } }
  }
});

globalThis.rc = {
  transform: function (src) {
    // babel's SyntaxError.message already carries the codeframe + "(line:col)"; rethrow it as a plain Error so
    // Graal's PolyglotException.getMessage() surfaces the full detail to Java instead of a bare "SyntaxError"
    try {
      return Babel.transform(src, {
        filename: 'input.jsx', // the compiler refuses to run without a filename
        // plugins run before presets: the compiler and autoMemo see the original JSX, then preset-react
        // lowers it to React.createElement (classic runtime against the platform window.React, no dev metadata)
        presets: [['react', {runtime: 'classic', pragma: 'React.createElement', pragmaFrag: 'React.Fragment', development: false}]],
        plugins: [[compiler.default || compiler, {target: '18', panicThreshold: 'none'}], autoMemo, rewriteRuntimeImports],
        parserOpts: {plugins: ['jsx']},
        sourceType: 'module'
      }).code;
    } catch (err) {
      throw new Error(String(err && err.message || err));
    }
  }
};
`;
fs.writeFileSync(path.join(WORK, 'entry.js'), ENTRY);

// node_modules/.bin/esbuild: on POSIX a directly executable file (esbuild's postinstall swaps its JS
// launcher for the native binary), on Windows a .cmd shim (spawned through the shell, see run above)
const OUT = path.join(DIR, 'babel-rc.min.js');
run(path.join(WORK, 'node_modules', '.bin', process.platform === 'win32' ? 'esbuild.cmd' : 'esbuild'),
    ['entry.js', '--bundle', '--minify', '--format=iife', '--platform=browser',
    '--define:require.resolve=__rcResolveShim',
    '--alias:os=./stubs/os.js', '--alias:tty=./stubs/tty.js', '--alias:util=./stubs/util.js',
    '--alias:fs=./stubs/fs.js', '--alias:path=./stubs/path.js', '--alias:buffer=./stubs/buffer.js',
    '--alias:crypto=./stubs/crypto.js', '--outfile=' + OUT, '--log-level=error']);

const bundle = fs.readFileSync(OUT);
fs.rmSync(WORK, {recursive: true, force: true});
console.log(`built: ${OUT} (${bundle.length} bytes, sha256 ${createHash('sha256').update(bundle).digest('hex')})`);
