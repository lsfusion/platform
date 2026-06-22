---
slug: "/System_Security"
title: 'Security'
---

`Security` is a [system module](System_modules.md) that defines the access-control model: user roles, role assignment, the permission set on properties, actions, property groups, and navigator elements, the per-role form and client settings, client memory limits, and the navigator-search form. It backs the [`Security policy`](Security_policy.md) management-side article, whose roles-and-access user interface is built on the classes, properties, and forms declared here. It is pulled in via `REQUIRE Security` (`System`, `SystemEvents`, `Email`, and `Utils` are pulled in automatically as its requirements).

### Roles

A role groups a set of permissions and settings that are then applied to the users it is assigned to.

| Class / property                  | What it is                                                                 |
|-----------------------------------|-----------------------------------------------------------------------------|
| `UserRole`                        | the role class                                                              |
| `name[UserRole]`                  | role name (`ISTRING[100]`)                                                  |
| `sid[UserRole]`                   | role code (`STRING[30]`), the stable identifier used to look a role up      |
| `userRoleSID[STRING]`             | the role with the given `sid`                                              |
| `disableRole[UserRole]`           | flag turning off the role's access rights without un-assigning it           |
| `copy[UserRole, UserRole]`        | abstract action that copies one role's settings into another (extended by every settings block below); the base implementation sets the target's `name` and `sid` to the source value with a `_copy` suffix |
| `copy[UserRole]`                  | creates a new role, fills it through `copy[UserRole, UserRole]`, and activates it on the `securityPolicy` form |

A role cannot be deleted while it is still assigned to any user (`CONSTRAINT` on `DROPPED`).

### Role assignment

A user has one main role and any number of additional roles; the effective membership combines both.

| Property                          | What it returns                                                            |
|-----------------------------------|-----------------------------------------------------------------------------|
| `mainRole[User]`                  | the user's main role                                                        |
| `nameMainRole[User]` / `sidMainRole[CustomUser]` | name / code of the main role                                |
| `currentUserMainRoleName[]`       | name of the current user's main role                                       |
| `overIn[User, UserRole]`          | abstract membership flag; the base implementation adds the main role (`mainRole(user) == role`) |
| `in[CustomUser, UserRole]`        | explicit assignment of an additional role                                  |
| `has[User, UserRole]`             | effective membership — `in[CustomUser, UserRole]` or `overIn[User, UserRole]` (materialized) |
| `userRoles[User]`                 | comma-separated names of all roles the user has, ordered by name           |
| `firstRole[User]`                 | the lowest-numbered role the user has other than `default`, falling back to the `default` role |
| `rolesCount[User]`                | number of roles the user has                                               |

### System roles

`createSystemUserRoles[]` creates the four built-in roles if they are absent, identified by `sid`:

| `sid`          | Name            | Initial rights                                                         |
|----------------|-----------------|------------------------------------------------------------------------|
| `admin`        | Administrator   | shows detailed info; permits the `System.root` navigator folder and the view / change / edit-objects permissions on the `System_root` property group |
| `readonly`     | Read only       | forbids the change permission on the `System_root` property group       |
| `default`      | Default         | no initial rights                                                       |
| `selfRegister` | New user        | forbids `System.root`, permits the `Authentication.account` folder      |

Assignment is wired to user events: when a user is created (`SETCHANGED` on `User`), the `default` role is assigned, and the `admin` role too when the login equals `admin`; on self-registration (`onUserRegister`) the `selfRegister` role is assigned.

### Permissions

Access is expressed by the `Permission` enum class with values `permit`, `forbid`, and `default`. A permission is stored per role on three kinds of targets — a property group (`PropertyGroup`), an individual action or property (`ActionOrProperty`), and a navigator element (`NavigatorElement`) — and inherited down their parent hierarchies.

The stored value is `dataPermission…[UserRole, …]`; the effective value `permission…[UserRole, …]` is the stored value, or, when absent, `nearestParentPermission…` — the permission of the nearest ancestor in the group / element tree that has one set (`GROUP LAST … ORDER DESC level`). `captionPermission…` is the displayed permission (defaulting to `Permission.default`); `foregroundPermission…` greys out inherited cells and `backgroundPermission…` marks a parent whose descendants carry a differing permission.

Property access splits into four permission types, generated by the `@initPropertyPermission` metacode for both the property-group and the action-or-property targets:

| Type          | Caption          | Controls                                                        |
|---------------|------------------|----------------------------------------------------------------|
| `view`        | View             | whether the property is visible                                 |
| `change`      | Change           | whether the property value can be changed                      |
| `editObjects` | Follow           | whether an object held in the property value can be opened     |
| `groupChange` | Group change     | whether the property can be changed for a group of objects     |

Each type yields a parallel family of properties, for example `dataPermissionView[UserRole, PropertyGroup]`, `permissionView[UserRole, ActionOrProperty]`, and `captionPermissionView[UserRole, ActionOrProperty]`. Crossing the 100-changes threshold in a single transaction drops the cached security policy for the role rather than updating each entry individually.

Navigator access uses the same single-permission model (`dataPermission[UserRole, NavigatorElement]`, `permission[UserRole, NavigatorElement]`, `captionPermission[UserRole, NavigatorElement]` captioned `Access`). The effective per-user access is `permit[User, NavigatorElement]`: the element is permitted when some enabled role of the user permits it, or when no enabled role forbids it.

### Form and client settings

These flags are set per role and combined across the user's roles.

| Property                                     | What it does                                                              |
|----------------------------------------------|----------------------------------------------------------------------------|
| `forbidDuplicateForms[UserRole]`             | open a single instance of an already-open form instead of a new window     |
| `dataForbidDuplicateForms[CustomUser]`       | the same restriction set on an individual user                            |
| `autoReconnectOnConnectionLost[UserRole]`    | reconnect the client automatically after a lost connection                 |
| `showDetailedInfo[UserRole]`                 | show the detailed description popup for properties and controls            |
| `maximizeDefaultForms[UserRole]`             | open the client maximized (navigator hidden) at startup                    |

#### Default forms

`defaultNumber[UserRole, NavigatorElement]` gives a startup order number to a navigator element for a role; `defaultNumber[User, NavigatorElement]` takes the minimum across the user's roles. `defaultForms[UserRole]` lists the captions of a role's default forms in that order. `mobileOnly[UserRole, NavigatorElement]` (a `YesNo` value, shown via `captionMobileOnly`) restricts a default form to mobile or to desktop only.

`showDefaultForms[]` opens these forms in order on client startup — running each navigator action, or showing each form with `SHOW … NOWAIT` — and maximizes the window when `maximizeDefaultForms` is set for any of the current user's roles. It runs on both desktop and web client start (`onDesktopClientStarted`, `onWebClientStarted`).

The `@addRoleApplicationSetting` metacode is an extension point that declares an additional per-role setting property, places it on the `securityPolicy` form's application-settings pane, and extends `copy[UserRole, UserRole]` to carry it.

### Client launch and memory

`initHeapSize[]`, `maxHeapSize[]`, `minHeapFreeRatio[]`, `maxHeapFreeRatio[]`, and `vmargs[]` hold the JVM parameters passed to the desktop client launcher.

The `MemoryLimit` class names a set of launch parameters — `name[MemoryLimit]`, `maxHeapSize[MemoryLimit]`, `vmargs[MemoryLimit]` — for a ready-made client-launch option. `generateJnlpUrls[]` builds, into `exportText[]`, the HTML list of desktop-client launch links, one per `MemoryLimit`, each carrying its heap size and VM arguments.

### Navigator search

The `findNavigator` form shows the navigator as a tree and lets the user type into `findText[]` to filter elements whose caption contains the text and that the current user is permitted to open (`permit[User, NavigatorElement]`); `openForm[NavigatorElement]` shows the selected form (`SHOW … NOWAIT`). It is added to the navigator's system window as a search entry.

### Reflection extensions

The module extends the `forms` reflection form with per-role figures: `countUserPreferences[GroupObject, UserRole]` counts users of the role who have saved preferences for a group object, and `countUser[PropertyDraw, UserRole, PropertyDrawShowStatus]` counts users of the role by the show status of a property draw. `captionShow[PropertyDraw, UserRole]` summarizes those counts; `hide[PropertyDraw, UserRole]`, `columnSort[PropertyDraw, UserRole]`, and `dropSort[PropertyDraw, UserRole]` apply or clear the corresponding setting for every user of the role.

### Forms

| Form               | Purpose                                                                       |
|--------------------|-------------------------------------------------------------------------------|
| `securityPolicy`   | the main access-policy form: roles with their settings, the navigator-element and property trees with their permission columns, and the role's users |
| `propertyPolicy`   | per-property policy (loggable, set-not-null, input-list, select) and its permissions across roles |
| `actionPolicy`     | per-action permissions across roles                                          |
| `userRolesDialog`  | a role-picker dialog                                                          |
| `findNavigator`    | the navigator-search form                                                     |

### Language

- [`META` statement](../language/META_statement.md) — the metacode mechanism behind the generated property-permission families (`@initPropertyPermission`) and the per-role setting extension point (`@addRoleApplicationSetting`).

### See also

- [`Security policy`](Security_policy.md) — the management-side article on configuring roles and access; this module is its standard-library backing.
- [`System modules`](System_modules.md) — the general list of platform modules.
- [`Authentication`](System_Authentication.md) — users, contacts, and sign-in, whose `User` / `CustomUser` classes roles are assigned to.
- [`Reflection`](System_Reflection.md) — metadata about the navigator, forms, and properties that permissions are set on.
