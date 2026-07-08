#!/usr/bin/env node
// (Re)build the self-contained React Compiler bundle (babel-standalone + babel-plugin-react-compiler + the
// autoMemo plugin, bundled by esbuild into one minified browser-style IIFE) into the core's resources
// (src/main/resources/rc/rc-graal.cjs, COMMITTED — ships inside the web-compile-maven-plugin jar together with
// its rc/PROVENANCE pin, and is evaluated IN-PROCESS by GraalJS at build time, see CompileWebMojo.rcTransform; no Node
// anywhere in a consumer's build). Requires node+npm (run: node bin/build-rc-graal.mjs, any OS); run only when
// bumping the pinned versions below.
//
// GraalJS constraints baked in here (each was hit while porting off Node):
// - babel-plugin-react-compiler pulls node builtins (os/tty/util/fs/path/buffer/crypto): aliased to the tiny
//   stubs written below (crypto.createHash is used only for cache ids, a deterministic FNV hash suffices);
// - its require.resolve('react-compiler-runtime') existence probe: --define'd to a shim returning the name
//   (pretend resolvable — the import itself is aliased to the vendored polyfill at bundle time, see CompileWebMojo);
// - the host must predefine window/global/process before evaluating the IIFE (done in CompileWebMojo.rcTransform).
import {spawnSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const BIN = path.dirname(fileURLToPath(import.meta.url));
const DIR = path.resolve(BIN, '..', 'src', 'main', 'resources', 'rc');
const LOCK = path.join(BIN, 'rc-graal.package-lock.json'); // committed npm lockfile = the pinned transitive graph of the bundle inputs
const BABEL_STANDALONE = '7.29.7';
const REACT_COMPILER = '1.0.0';
const ESBUILD = '0.25.10';
const WORK = fs.mkdtempSync(path.join(os.tmpdir(), 'rc-graal-'));

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
let pkg = {name: 'rc-graal-bundle', version: '1.0.0', license: 'ISC'};
if (fs.existsSync(LOCK)) {
    fs.copyFileSync(LOCK, path.join(WORK, 'package-lock.json'));
    const lockRoot = JSON.parse(fs.readFileSync(LOCK, 'utf8')).packages[''];
    pkg = {name: lockRoot.name, version: lockRoot.version, license: lockRoot.license};
}
fs.writeFileSync(path.join(WORK, 'package.json'), JSON.stringify(pkg, null, 2) + '\n');
run(NPM, ['install', '--no-audit', '--no-fund',
    '@babel/standalone@' + BABEL_STANDALONE, 'babel-plugin-react-compiler@' + REACT_COMPILER, 'esbuild@' + ESBUILD], true);
fs.copyFileSync(path.join(WORK, 'package-lock.json'), LOCK);

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

const ENTRY = String.raw`// require.resolve('react-compiler-runtime') existence probe -> pretend resolvable (see --define in the build script)
globalThis.__rcResolveShim = function (m) { return m; };
const Babel = require('@babel/standalone');
const compiler = require('babel-plugin-react-compiler');

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

// the host-visible entry point: CompileWebMojo.rcTransform(source, filename, target) — filename drives parser
// selection AND is required by babel-plugin-react-compiler (it refuses to run without one)
globalThis.rcTransform = function (source, filename, target) {
  // babel's SyntaxError.message already carries the codeframe + "(line:col)"; rethrow it as a plain Error so
  // Graal's PolyglotException.getMessage() surfaces the full detail to Java instead of a bare "SyntaxError"
  try {
    return Babel.transform(source, {
      filename: filename,
      parserOpts: { plugins: [/\.ts$/.test(filename) ? null : 'jsx', /\.tsx?$/.test(filename) ? 'typescript' : null].filter(Boolean) }, // plain .ts: jsx plugin breaks generic arrows (<T>(x) => ...)
      plugins: [[compiler.default || compiler, { target: String(target || '18'), panicThreshold: 'none' }], autoMemo],
      babelrc: false, configFile: false,
      presets: []
    }).code;
  } catch (err) {
    throw new Error(String(err && err.message || err));
  }
};
`;
fs.writeFileSync(path.join(WORK, 'entry.js'), ENTRY);

// node_modules/.bin/esbuild: on POSIX a directly executable file (esbuild's postinstall swaps its JS
// launcher for the native binary), on Windows a .cmd shim (spawned through the shell, see run above)
const OUT = path.join(DIR, 'rc-graal.cjs');
run(path.join(WORK, 'node_modules', '.bin', process.platform === 'win32' ? 'esbuild.cmd' : 'esbuild'),
    ['entry.js', '--bundle', '--minify', '--format=iife', '--platform=browser',
    '--define:require.resolve=__rcResolveShim',
    '--alias:os=./stubs/os.js', '--alias:tty=./stubs/tty.js', '--alias:util=./stubs/util.js',
    '--alias:fs=./stubs/fs.js', '--alias:path=./stubs/path.js', '--alias:buffer=./stubs/buffer.js',
    '--alias:crypto=./stubs/crypto.js',
    '--outfile=' + OUT, '--log-level=error']);

const bundle = fs.readFileSync(OUT);
const SHA = createHash('sha256').update(bundle).digest('hex');
const SIZE = bundle.length;
// license inventory of what actually gets bundled: every installed package except esbuild (bundler only)
const seen = [];
(function walk(dir, prefix) {
    for (const e of fs.readdirSync(dir)) {
        if (e.startsWith('.') || e.startsWith('esbuild') || e === '@esbuild') continue;
        const p = path.join(dir, e);
        if (e.startsWith('@')) { walk(p, e + '/'); continue; }
        const pj = path.join(p, 'package.json');
        if (fs.existsSync(pj)) {
            const m = JSON.parse(fs.readFileSync(pj, 'utf8'));
            seen.push('  ' + prefix + e + '@' + m.version + ': ' + (m.license || 'UNKNOWN'));
        }
    }
})(path.join(WORK, 'node_modules'), '');
const LICENSES = seen.sort().join('\n');
fs.writeFileSync(path.join(DIR, 'PROVENANCE'), `rc-graal.cjs — React Compiler bundle evaluated in-process by GraalJS (CompileWebMojo.rcTransform).
Rebuilt only by bin/build-rc-graal.mjs; this file pins what the committed bundle was built from.

npm inputs (exact versions):
  @babel/standalone@${BABEL_STANDALONE}
  babel-plugin-react-compiler@${REACT_COMPILER}
  esbuild@${ESBUILD} (bundler only, not part of the output)
transitive graph: pinned by bin/rc-graal.package-lock.json (committed; the script reuses it on rebuild,
  delete it when bumping the versions above)

licenses of the bundled packages (from their package.json; esbuild excluded — bundler only):
${LICENSES}

build: bin/build-rc-graal.mjs (esbuild --bundle --minify --format=iife --platform=browser,
  node builtins stubbed, require.resolve defined away — see the script for the full recipe)

size(rc-graal.cjs) = ${SIZE} bytes
sha256(rc-graal.cjs) = ${SHA}
`);
fs.rmSync(WORK, {recursive: true, force: true});
console.log(`built: ${OUT} (${SIZE} bytes, sha256 ${SHA})`);
