package lsfusion.build.web;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compiles a logic module's {@code src/main/web/**} (except the {@code web/lib/**} library subfolder) into
 * classpath {@code web/.compiled/*.js} IIFE bundles, one per public-entry file, using the esbuild native
 * binary resolved as an os-matched Maven artifact (no Node; the protoc pattern). Each bundle registers every named export of its entry file into
 * {@code window.lsfusion.custom} at load time; the lsFusion client resolves CUSTOM / CUSTOM REACT /
 * INTERNAL CLIENT names registry-first (see the runtime registry shim). The generated-resources root is
 * added as a project resource so Maven's process-resources packages it under classpath {@code web/.compiled/}.
 */
@Mojo(name = "compile-web", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class CompileWebMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/web")
    private File sourceDir;

    @Parameter(defaultValue = "${project.build.directory}/generated-resources/web-compiled")
    private File generatedRoot;

    @Parameter(defaultValue = "${project.build.directory}/web-compile-work")
    private File workDir;

    /** react / react-dom resolved to the platform global (window.React) via an alias shim. */
    @Parameter(defaultValue = "true")
    private boolean reactExternal;

    @Parameter(defaultValue = "false")
    private boolean minify;

    @Parameter(property = "lsfusion.web.skip", defaultValue = "false")
    private boolean skip;

    /** opt-in React Compiler (auto-memoization): babel-plugin-react-compiler transforms every source before
     * bundling. Requires `node` on PATH and a self-contained runner script (see bin/build-rc-runner.sh). */
    @Parameter(defaultValue = "false")
    private boolean reactCompiler;

    /** optional override of the React Compiler runner (single .cjs); by default the runner vendored inside the
     * plugin jar (src/main/resources/rc/rc-runner.cjs, rebuilt via bin/build-rc-runner.sh) is extracted and used */
    @Parameter
    private File reactCompilerRunner;

    /** react major the compiled output targets ('18' emits the react-compiler-runtime polyfill import) */
    @Parameter(defaultValue = "18")
    private String reactCompilerTarget;

    /** version of the esbuild binary artifact {@code org.mvnpm.at.esbuild:<os-arch>} (Maven Central mirror of npm);
     * effectively locked to the versions pinned in the plugin's SHA256SUMS — overriding requires a matching entry */
    @Parameter(defaultValue = "0.25.10")
    private String esbuildVersion;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepos;

    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("web-compile: skipped (lsfusion.web.skip)");
            return;
        }
        File outputDir = new File(generatedRoot, "web/.compiled");
        try {
            // clean BOTH the generated-resources output and its already-copied counterpart in target/classes:
            // maven-resources-plugin only copies, never deletes, so a renamed/deleted source would otherwise leave a
            // stale bundle on the classpath that the runtime auto-scan keeps loading (duplicate-name page failure).
            if (outputDir.exists())
                deleteRecursively(outputDir.toPath());
            File copiedDir = new File(project.getBuild().getOutputDirectory(), "web/.compiled");
            if (copiedDir.exists())
                deleteRecursively(copiedDir.toPath());
            if (sourceDir == null || !sourceDir.isDirectory()) {
                getLog().debug("web-compile: no " + sourceDir + ", nothing to compile");
                return;
            }
            Files.createDirectories(workDir.toPath());
            Files.createDirectories(outputDir.toPath());

            File esbuild = resolveEsbuild();
            File reactShim = extractResource("shims/react.js", new File(workDir, "react.js"));
            File reactDomShim = extractResource("shims/react-dom-client.js", new File(workDir, "react-dom-client.js"));
            File reactDomBareShim = extractResource("shims/react-dom.js", new File(workDir, "react-dom.js"));
            File jsxRuntimeShim = extractResource("shims/react-jsx-runtime.js", new File(workDir, "react-jsx-runtime.js"));
            Map<String, File> mvnpmPackages = extractMvnpmNodeModules(new File(workDir, "node_modules")); // org.mvnpm:* deps -> bundlable packages

            Path srcRoot = sourceDir.toPath().toAbsolutePath();
            if (reactCompiler)
                srcRoot = runReactCompiler(srcRoot); // transformed mirror: ALL sources (incl. lib/) pass through the compiler
            Path libDir = srcRoot.resolve("lib");
            List<Path> entries;
            try (Stream<Path> walk = Files.walk(srcRoot)) {
                entries = walk.filter(Files::isRegularFile)
                        .filter(CompileWebMojo::isCompilable)
                        .filter(p -> !p.startsWith(libDir))
                        .sorted()
                        .collect(Collectors.toList());
            }
            if (entries.isEmpty()) {
                getLog().info("web-compile: no entry files under " + srcRoot);
                addGeneratedResource();
                return;
            }

            List<String> bundleNames = new ArrayList<>();
            java.util.Map<String, Path> stems = new java.util.HashMap<>();
            for (Path entry : entries) {
                String stem = stemOf(srcRoot.relativize(entry));
                // case-insensitive clash key: Order.jsx vs order.jsx would silently overwrite on win/mac filesystems
                Path clash = stems.put(stem.toLowerCase(Locale.ROOT), entry);
                if (clash != null)
                    throw new MojoExecutionException("output name collision: '" + stem + ".js' from both "
                            + srcRoot.relativize(clash) + " and " + srcRoot.relativize(entry));
                File wrapper = new File(workDir, "entry_" + stem + ".js");
                // register every named export (except default) of the entry at load time; the entry's own
                // imports (react -> shim, helpers from js/lib) are bundled in. esbuild keeps namespace keys.
                // per-name catch: one duplicate must not silently abort the bundle's remaining registrations
                String wrapperJs = "import * as __m from " + jsString(entry.toString()) + ";\n"
                        + "Object.keys(__m).forEach(function (k) { if (k === 'default') return; try { window.lsfusion.custom.register(k, __m[k]); } catch (e) { console.error(e); } });\n";
                Files.write(wrapper.toPath(), wrapperJs.getBytes(StandardCharsets.UTF_8));

                File out = new File(outputDir, stem + ".js");
                List<String> cmd = new ArrayList<>();
                cmd.add(esbuild.getAbsolutePath());
                cmd.add(wrapper.getAbsolutePath());
                cmd.add("--bundle");
                cmd.add("--format=iife");
                cmd.add("--jsx=transform");
                cmd.add("--loader:.js=jsx"); // allow JSX in plain .js (esbuild otherwise rejects it)
                // url() assets (fonts/images) referenced from imported CSS: INLINE them into the extracted .css as data URLs,
                // so the compiled stylesheet is self-contained and served as one registered resource (the platform publishes
                // only registered resources, not arbitrary sibling files; esbuild has no default loader for these binary types)
                for (String ext : new String[]{"woff", "woff2", "ttf", "eot", "otf", "svg", "png", "jpg", "jpeg", "gif", "webp"})
                    cmd.add("--loader:." + ext + "=dataurl");
                if (reactExternal) {
                    cmd.add("--alias:react=" + reactShim.getAbsolutePath());
                    cmd.add("--alias:react-dom/client=" + reactDomShim.getAbsolutePath());
                    cmd.add("--alias:react-dom=" + reactDomBareShim.getAbsolutePath());
                    // a 'react' alias rewrites the subpath react/jsx-runtime -> react.js/jsx-runtime, so the
                    // automatic JSX runtime (used by many third-party packages) needs its own alias
                    cmd.add("--alias:react/jsx-runtime=" + jsxRuntimeShim.getAbsolutePath());
                    cmd.add("--alias:react/jsx-dev-runtime=" + jsxRuntimeShim.getAbsolutePath());
                }
                if (reactCompiler) { // compiled components import { c } from the memo-cache runtime: target 18 emits the
                    // react-compiler-runtime polyfill package name, react 19 style is react/compiler-runtime — alias both to the vendored MIT polyfill
                    String rcRuntime = extractResource("shims/react-compiler-runtime.js", new File(workDir, "react-compiler-runtime.js")).getAbsolutePath();
                    cmd.add("--alias:react-compiler-runtime=" + rcRuntime);
                    cmd.add("--alias:react/compiler-runtime=" + rcRuntime);
                }
                for (Map.Entry<String, File> pkg : mvnpmPackages.entrySet()) // bare imports declared as org.mvnpm Maven deps
                    cmd.add("--alias:" + pkg.getKey() + "=" + pkg.getValue().getAbsolutePath());
                if (minify)
                    cmd.add("--minify");
                cmd.add("--outfile=" + out.getAbsolutePath());
                runEsbuild(cmd);

                File cssOut = new File(outputDir, stem + ".css");
                if (cssOut.exists()) // esbuild extracts imported CSS to a sibling file; the runtime auto-scan (SystemEvents) auto-loads web/.compiled/*.css too
                    getLog().info("web-compile: " + srcRoot.relativize(entry) + " -> web/.compiled/" + stem + ".css");

                bundleNames.add("web/.compiled/" + stem + ".js");
                getLog().info("web-compile: " + srcRoot.relativize(entry) + " -> web/.compiled/" + stem + ".js");
            }

            String manifest = "{\"bundles\":["
                    + bundleNames.stream().map(CompileWebMojo::jsString).collect(Collectors.joining(","))
                    + "]}";
            Files.write(new File(outputDir, "manifest.json").toPath(), manifest.getBytes(StandardCharsets.UTF_8));

            addGeneratedResource();
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("web-compile failed", e);
        }
    }

    private void addGeneratedResource() {
        for (Resource r : project.getResources())
            if (generatedRoot.getAbsolutePath().equals(new File(r.getDirectory()).getAbsolutePath()))
                return;
        Resource resource = new Resource();
        resource.setDirectory(generatedRoot.getAbsolutePath());
        project.addResource(resource);
    }

    private void runEsbuild(List<String> cmd) throws MojoExecutionException, IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output;
        try (InputStream is = p.getInputStream()) {
            output = new String(readAll(is), StandardCharsets.UTF_8);
        }
        int code = p.waitFor();
        if (!output.isEmpty())
            getLog().info("esbuild: " + output); // warnings must be visible, not hidden behind -X
        if (code != 0)
            throw new MojoExecutionException("esbuild exit " + code + ":\n" + output);
    }

    // transform every compilable source through babel-plugin-react-compiler into a mirror tree (relative
    // imports keep resolving), which then serves as the bundling root. Runner = a self-contained .cjs
    // (babel core + the compiler plugin bundled by esbuild), executed with the system node.
    private Path runReactCompiler(Path srcRoot) throws MojoExecutionException, IOException, InterruptedException {
        File runner = reactCompilerRunner; // optional override (e.g. testing a rebuilt runner); default = the vendored one
        if (runner == null)
            runner = extractResource("rc/rc-runner.cjs", new File(workDir, "rc-runner.cjs"));
        else if (!runner.isFile())
            throw new MojoExecutionException("reactCompilerRunner not found: " + runner);
        Path mirror = new File(workDir, "rc-src").toPath();
        if (Files.exists(mirror))
            deleteRecursively(mirror);
        List<Path> sources;
        try (Stream<Path> walk = Files.walk(srcRoot)) {
            sources = walk.filter(Files::isRegularFile).collect(Collectors.toList());
        }
        for (Path src : sources) {
            Path out = mirror.resolve(srcRoot.relativize(src));
            Files.createDirectories(out.getParent());
            if (isCompilable(src)) {
                ProcessBuilder pb = new ProcessBuilder("node", runner.getAbsolutePath(),
                        src.toString(), out.toString(), reactCompilerTarget);
                pb.redirectErrorStream(true);
                Process p;
                try {
                    p = pb.start();
                } catch (IOException e) {
                    throw new MojoExecutionException("reactCompiler=true requires node on PATH", e);
                }
                String output;
                try (InputStream is = p.getInputStream()) {
                    output = new String(readAll(is), StandardCharsets.UTF_8);
                }
                if (p.waitFor() != 0)
                    throw new MojoExecutionException("react-compiler failed on " + srcRoot.relativize(src) + ":\n" + output);
            } else
                Files.copy(src, out, StandardCopyOption.REPLACE_EXISTING);
        }
        getLog().info("web-compile: react-compiler transformed " + sources.size() + " sources");
        return mirror;
    }

    // ===== mvnpm: materialize the project's org.mvnpm* dependencies (the Maven Central mirror of npm) into
    // workDir/node_modules, so a third-party library is added by ONE Maven coordinate (org.mvnpm:<pkg> or
    // org.mvnpm.at.<scope>:<name>) + an import - resolved offline from ~/.m2 by Maven (incl. the transitive npm
    // graph the mvnpm poms declare), no npm / no live npm registry. esbuild then bundles the bare imports.
    // Each package is ALSO aliased name -> absolute dir at the esbuild call, so it resolves no matter where the
    // importing source sits (the upward node_modules walk only reaches workDir/node_modules from workDir sources).
    private Map<String, File> extractMvnpmNodeModules(File nodeModules) throws MojoExecutionException, IOException {
        Map<String, File> packages = new LinkedHashMap<>();
        if (nodeModules.exists())
            deleteRecursively(nodeModules.toPath()); // a package removed from the pom must not linger
        for (Artifact artifact : project.getArtifacts()) {
            String gid = artifact.getGroupId();
            if (!gid.equals("org.mvnpm") && !gid.startsWith("org.mvnpm."))
                continue;
            File jar = artifact.getFile();
            if (jar == null || !jar.isFile())
                throw new MojoExecutionException("unresolved mvnpm artifact: " + artifact);
            String name = extractMvnpmJar(jar, nodeModules);
            if (name != null)
                packages.put(name, new File(nodeModules, name));
        }
        return packages;
    }

    // an mvnpm jar holds one npm package under META-INF/resources/_static/<path>/<version>/ (package.json at that
    // root); returns the npm name, or null when skipped (react/react-dom resolve to window.React via the shim).
    private String extractMvnpmJar(File jar, File nodeModules) throws MojoExecutionException, IOException {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar)) {
            String rootPkgJson = null; // the package root = the shallowest such package.json (nested ones are subdirs)
            for (java.util.Enumeration<? extends java.util.zip.ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
                String n = en.nextElement().getName();
                if (n.startsWith("META-INF/resources/_static/") && n.endsWith("/package.json")
                        && (rootPkgJson == null || n.length() < rootPkgJson.length()))
                    rootPkgJson = n;
            }
            if (rootPkgJson == null)
                throw new MojoExecutionException("no package.json under META-INF/resources/_static/ in " + jar.getName());
            String root = rootPkgJson.substring(0, rootPkgJson.length() - "package.json".length()); // trailing slash
            String name;
            try (InputStream is = zip.getInputStream(zip.getEntry(rootPkgJson))) {
                name = npmName(new String(readAll(is), StandardCharsets.UTF_8));
            }
            if (name == null || !name.matches("(@[^/]+/)?[^/]+"))
                throw new MojoExecutionException("missing/invalid npm name " + name + " in " + rootPkgJson + " of " + jar.getName());
            if (reactExternal && (name.equals("react") || name.equals("react-dom")))
                return null; // the platform's single React (window.React) via the alias shim - never a second copy

            File dest = new File(nodeModules, name); // scoped @scope/name -> nested dirs (File handles the slash)
            for (java.util.Enumeration<? extends java.util.zip.ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
                java.util.zip.ZipEntry e = en.nextElement();
                if (!e.isDirectory() && e.getName().startsWith(root))
                    writeStream(zip.getInputStream(e), safeChild(dest, e.getName().substring(root.length())));
            }
            // mvnpm overflows files past Maven Central's per-jar limits into META-INF/.more.tgz (typically .d.ts);
            // best-effort - a missing runtime module would surface as a loud esbuild "could not resolve"
            java.util.zip.ZipEntry more = zip.getEntry("META-INF/.more.tgz");
            if (more != null) {
                try (InputStream is = zip.getInputStream(more)) {
                    extractMoreTgz(is, root.substring("META-INF/".length()), dest); // tgz paths drop the META-INF/ prefix
                } catch (Exception ex) { // best-effort: a malformed overflow archive must warn, never fail the build
                    getLog().warn("web-compile: .more.tgz of " + name + " not unpacked (" + ex + ")");
                }
            }
            return name;
        }
    }

    private static String npmName(String packageJson) {
        Matcher m = Pattern.compile("\"name\"\\s*:\\s*\"((?:@[^\"/]+/)?[^\"]+)\"").matcher(packageJson);
        return m.find() ? m.group(1) : null;
    }

    // reject a relative path that escapes the package dir (zip/tar slip from an untrusted Maven artifact)
    private static File safeChild(File base, String relative) throws IOException {
        File out = new File(base, relative);
        String basePath = base.getCanonicalPath() + File.separator;
        if (!out.getCanonicalPath().startsWith(basePath) && !out.getCanonicalPath().equals(base.getCanonicalPath()))
            throw new IOException("entry escapes package dir: " + relative);
        return out;
    }
    private static void writeStream(InputStream is, File out) throws IOException {
        Files.createDirectories(out.toPath().getParent());
        try (InputStream in = is) {
            Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // minimal gunzip + ustar reader: extract regular-file entries under tgzRoot into dest (relative to tgzRoot)
    private static void extractMoreTgz(InputStream tgz, String tgzRoot, File dest) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(tgz)) {
            byte[] header = new byte[512];
            while (readFully(gz, header)) {
                boolean zero = true;
                for (byte b : header) if (b != 0) { zero = false; break; }
                if (zero) break; // a zero block terminates the archive
                String name = cString(header, 0, 100);
                long size = parseOctal(header, 124, 12);
                char type = (char) header[156];
                long pad = (512 - (size % 512)) % 512;
                if ((type == '0' || type == ' ') && name.startsWith(tgzRoot)) {
                    File out = safeChild(dest, name.substring(tgzRoot.length()));
                    Files.createDirectories(out.toPath().getParent());
                    try (java.io.OutputStream os = Files.newOutputStream(out.toPath())) {
                        copyN(gz, os, size);
                    }
                    skipN(gz, pad);
                } else
                    skipN(gz, size + pad); // dirs, pax/gnu meta, or files outside the package root
            }
        }
    }

    private static boolean readFully(InputStream is, byte[] buf) throws IOException {
        int off = 0;
        while (off < buf.length) {
            int r = is.read(buf, off, buf.length - off);
            if (r < 0) return false; // truncated / end of stream
            off += r;
        }
        return true;
    }
    private static String cString(byte[] b, int off, int len) {
        int end = off;
        while (end < off + len && b[end] != 0) end++;
        return new String(b, off, end - off, StandardCharsets.UTF_8);
    }
    private static long parseOctal(byte[] b, int off, int len) {
        String s = cString(b, off, len).trim();
        return s.isEmpty() ? 0 : Long.parseLong(s, 8);
    }
    private static void copyN(InputStream is, java.io.OutputStream os, long n) throws IOException {
        byte[] buf = new byte[8192];
        while (n > 0) {
            int r = is.read(buf, 0, (int) Math.min(buf.length, n));
            if (r < 0) break;
            os.write(buf, 0, r);
            n -= r;
        }
    }
    private static void skipN(InputStream is, long n) throws IOException {
        while (n > 0) {
            long s = is.skip(n);
            if (s <= 0) { if (is.read() < 0) break; n--; } else n -= s;
        }
    }

    private static final String ESBUILD_GROUP = "org.mvnpm.at.esbuild";

    // the esbuild binary ships inside the org.mvnpm.at.esbuild:<platform> jar on Maven Central (the mvnpm
    // mirror of the npm @esbuild packages), so nothing is hosted in our repos: the Mojo resolves the
    // os-matched jar into ~/.m2 (offline -o works once cached), pins its sha256, and extracts the executable.
    private File resolveEsbuild() throws MojoExecutionException, IOException {
        String platform = osArch();
        File jar;
        try {
            ArtifactRequest request = new ArtifactRequest(
                    new DefaultArtifact(ESBUILD_GROUP, platform, null, "jar", esbuildVersion),
                    remoteRepos, null);
            jar = repoSystem.resolveArtifact(repoSession, request).getArtifact().getFile();
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("cannot resolve the esbuild binary " + ESBUILD_GROUP + ":" + platform
                    + ":" + esbuildVersion + " from Maven Central (in offline mode, prefetch it once online)", e);
        }
        verifySha256(jar, platform);
        File target = new File(workDir, platform.startsWith("win32") ? "esbuild.exe" : "esbuild");
        Files.createDirectories(target.toPath().getParent());
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar)) {
            java.util.zip.ZipEntry bin = null;
            for (java.util.Enumeration<? extends java.util.zip.ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
                java.util.zip.ZipEntry e = en.nextElement();
                if (e.getName().endsWith("/bin/esbuild") || e.getName().endsWith("/esbuild.exe")) { bin = e; break; }
            }
            if (bin == null)
                throw new MojoExecutionException("no esbuild executable inside " + jar.getName());
            try (InputStream is = zip.getInputStream(bin)) {
                Files.copy(is, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (!target.setExecutable(true, false))
            getLog().warn("could not chmod +x " + target);
        return target;
    }

    // supply-chain pin: the resolved binary must match the sha256 committed in the plugin source (SHA256SUMS)
    private void verifySha256(File file, String classifier) throws MojoExecutionException, IOException {
        String key = esbuildVersion + "-" + classifier, expected = null;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("esbuild/SHA256SUMS")) {
            if (is != null)
                for (String line : new String(readAll(is), StandardCharsets.UTF_8).split("\n")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length == 2 && parts[1].equals(key))
                        expected = parts[0];
                }
        }
        if (expected == null)
            throw new MojoExecutionException("no sha256 entry for esbuild " + key + " in SHA256SUMS (add it when deploying the binary artifact)");
        String actual;
        try {
            actual = toHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath())));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new MojoExecutionException("SHA-256 unavailable", e);
        }
        if (!actual.equals(expected))
            throw new MojoExecutionException("esbuild binary sha256 mismatch for " + key + ": expected " + expected + ", got " + actual);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder b = new StringBuilder();
        for (byte x : bytes) b.append(String.format("%02x", x));
        return b.toString();
    }

    private File extractResource(String resource, File target) throws MojoExecutionException, IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (is == null)
                throw new MojoExecutionException("bundled resource not found: " + resource);
            Files.createDirectories(target.toPath().getParent());
            Files.copy(is, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static boolean isCompilable(Path p) {
        String n = p.getFileName().toString();
        return (n.endsWith(".js") || n.endsWith(".jsx") || n.endsWith(".ts") || n.endsWith(".tsx"))
                && !n.endsWith(".d.ts");
    }

    private static String stemOf(Path rel) {
        String s = rel.toString().replace(File.separatorChar, '/');
        s = s.replaceAll("\\.(jsx?|tsx?)$", "");
        return s.replace('/', '_');
    }

    private static String jsString(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\')
                b.append('\\').append(c);
            else if (c == '\n')
                b.append("\\n");
            else
                b.append(c);
        }
        return b.append('"').toString();
    }

    private static String osArch() throws MojoExecutionException {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        String a = (arch.contains("aarch64") || arch.contains("arm64")) ? "arm64" : "x64";
        if (os.contains("linux")) return "linux-" + a;
        if (os.contains("mac") || os.contains("darwin")) return "darwin-" + a;
        if (os.contains("win")) return "win32-x64"; // win32-arm64 not published for all versions; Windows-on-ARM runs x64 via emulation
        throw new MojoExecutionException("unsupported OS for esbuild: " + os + " " + arch);
    }

    private static byte[] readAll(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList()))
                Files.delete(p); // propagate: a silently-undeleted file means stale output on the classpath
        }
    }
}
