---
title: 'Projects'
---

*Project* is a collection of [modules](Modules.md) and additional information (pictures, report design files, etc.) that fully describe the functionality of the information system being created.

Projects, like modules, can depend on each other. A project graph should "include" a module graph. That is, if module `A` [depends](Modules.md#depends) on module `B`, then module `A` project should depend on module `B` project.

Also, as a rule, assembly automation and versioning are supported for projects (for example, assembling a single executable file with all dependencies).

### Language

From a technical standpoint, a project is nothing more than a set of files, so project support is not directly a part of the platform. It is assumed that external tools are used for this, from simple built-in IDEs to complex multi-purpose frameworks (such as [Maven](https://maven.apache.org/)).

By default, at startup the platform searches for all files with the lsf extension in the [classpath](Launch_parameters.md#appjava) of the application server being started, and considers them loaded modules. Modules are loaded in the order of their [dependencies](Modules.md#depends), so if `A` depends on `B` and `C`, then by default `B` is initialized first, then `C`, and only then `A`.

The above behavior can however be changed using the appropriate lsFusion application server startup parameters:

-   [`logics.includePaths`, `logics.excludePaths`](Launch_parameters.md#project) - the paths (relative to the classpath) in which the platform will look for LSF files. When specifying these parameters you can use paths to specific files (for example, `A/B/C.lsf`) as well as path templates (for example, `A/*` - all LSF files in folder `A` and all its subfolders). In addition, in these parameters you can specify several paths/path patterns at once, separated by semicolons - for example, `A.lsf; dirB/*`. The name of a JAR file in the path is ignored (that is, a file in `b.jar/C/x.lsf` is considered to have the path `C/x.lsf`). By default, `includePaths` is equal to `*` (that is, all files), and `excludedPaths` is empty.
-   [`logics.topModule`](Launch_parameters.md#project) - the name of the top module. If this parameter is specified (not empty), not all LSF files will be loaded, but only the specified module and all its [dependencies](Modules.md#depends). By default, this parameter is considered not specified (empty).
-   [`logics.orderDependencies`](Launch_parameters.md#project) - redefinition of the order of dependencies (set as module names separated by commas). Thus, if `A` depends on `B` and `C`, and this parameter contains `B` and `C`, with `C` placed before `B`, then `C` will be initialized before `B`. By default, this parameter is considered not specified (empty); that is, the `REQUIRE` order is used in the LSF files themselves.

Regardless of the parameters described above, the platform always automatically loads the following system modules: [`System`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/System.lsf), [`Service`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Service.lsf), [`Reflection`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Reflection.lsf), [`Authentication`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Authentication.lsf), [`Security`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Security.lsf), [`SystemEvents`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/SystemEvents.lsf), [`Scheduler`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Scheduler.lsf), [`Email`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Email.lsf), [`Time`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Time.lsf) and [`Utils`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/Utils.lsf).
