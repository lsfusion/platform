# babel-rc.min.js provenance

Vendored bundle of [Babel](https://babeljs.io) (`@babel/standalone`) plus
[babel-plugin-react-compiler](https://www.npmjs.com/package/babel-plugin-react-compiler), used by
`lsfusion.gwt.server.JsxTransformer` to transform lightweight-tier `.jsx` resources to plain js on
the server (in an embedded GraalJS engine) when the resource is saved for serving: one Babel pass
does the JSX transform (classic runtime, `React.createElement`), React Compiler auto-memoization
(memo cache against the `window.lsfusion.rcRuntime` shim, see
`server/src/main/resources/web/lsfusion-rc-runtime.js`), and an automatic `React.memo` wrap of
compiler-certified components. It is an internal classpath resource of the web client — it is never
served to the browser.

## Coordinates

- npm packages: `@babel/standalone@7.29.7` (MIT), `babel-plugin-react-compiler@1.0.0` (MIT)
- sha256 of `babel-rc.min.js`: `ef3e7f1efc352aeee99ea0e8065db448315f7c9d294cc0dd7d1c452848464fb6`
- size: 5339230 bytes
- full transitive pin: `babel-rc.package-lock.json` (the `package-lock.json` of the bundle build,
  captured by the build script; includes the build-time-only esbuild)

## Reproducible build

```
node web-client/bin/build-babel-rc.mjs
```

The script pins the versions above, generates the bundle entry (the `autoMemo` and
`rewriteRuntimeImports` Babel plugins plus the `globalThis.rc.transform(src)` wrapper) and the
node-builtin stubs, and bundles everything with esbuild:

```
esbuild entry.js --bundle --minify --format=iife --platform=browser \
    --define:require.resolve=__rcResolveShim \
    --alias:os=./stubs/os.js --alias:tty=./stubs/tty.js --alias:util=./stubs/util.js \
    --alias:fs=./stubs/fs.js --alias:path=./stubs/path.js --alias:buffer=./stubs/buffer.js \
    --alias:crypto=./stubs/crypto.js --outfile=babel-rc.min.js
```

- esbuild: 0.25.10 (linux-x64 native binary), node v18.19.1 / npm 9.2.0
- GraalJS compatibility: `babel-plugin-react-compiler` pulls node builtins (os/tty/util/fs/path/
  buffer/crypto) — stubbed via the esbuild aliases (crypto's `createHash` is used only for cache
  ids, so a deterministic non-crypto hash suffices); its `require.resolve('react-compiler-runtime')`
  probe is `--define`d to a shim that pretends the module is resolvable. The consumer must define
  `window`/`global`/`process` before evaluating the bundle (see `JsxTransformer`).
- Transitive npm deps resolved at build time (2026-07-08), pinned exactly in
  `babel-rc.package-lock.json`: `@babel/types@7.29.7`, `@babel/helper-string-parser@7.29.7`,
  `@babel/helper-validator-identifier@7.29.7`.

## License inventory

Everything that ends up inside the bundle is MIT-licensed:

| package (bundled) | license |
|---|---|
| `@babel/standalone@7.29.7` | MIT |
| `babel-plugin-react-compiler@1.0.0` | MIT |
| `@babel/types@7.29.7` | MIT |
| `@babel/helper-string-parser@7.29.7` | MIT |
| `@babel/helper-validator-identifier@7.29.7` | MIT |

Build-time only (not part of the bundle): `esbuild@0.25.10` (MIT).

The bundle is an iife exposing the global `rc` with `transform(source)` returning the transformed
code (a string).

## License

Babel and babel-plugin-react-compiler are distributed under the MIT license:

```
MIT License

Copyright (c) 2014-present Sebastian McKenzie and other contributors
Copyright (c) Meta Platforms, Inc. and affiliates.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```
