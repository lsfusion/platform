---
slug: "/System_Authentication"
title: 'Authentication'
---

`Authentication` is a [system module](System_modules.md) that declares the users of the application and everything around signing them in: the user and contact [classes](User_classes.md), passwords and authentication tokens, the current-user properties, the locale each user works in, the design environment and color theme, the external authentication providers (LDAP, OAuth2), the HTTP API endpoints for registration and password recovery, the session-management actions, and object locks. It is pulled in via `REQUIRE Authentication` (it itself does `REQUIRE Time, Utils`).

### Design environment

A *design environment* is the entity that personal interface settings (navigator layout, color theme) are stored against. It is either the current user or the current computer, depending on the `storeNavigatorSettingsForComputer[]` flag.

| Class / property                          | What it is                                                                                  |
|-------------------------------------------|----------------------------------------------------------------------------------------------|
| `DesignEnv`                               | the abstract base class of design environments; `Computer` and `User` both inherit from it  |
| `currentDesignEnv[]`                      | the current design environment: `currentComputer[]` when `storeNavigatorSettingsForComputer[]` is set, otherwise `currentUser[]` |
| `ColorTheme`                              | a static class with two objects, `light` and `dark`                                         |
| `designEnvColorTheme[DesignEnv]`          | the color theme chosen for a design environment                                             |
| `colorTheme[DesignEnv]`                   | the resolved theme: `designEnvColorTheme[DesignEnv]` if set, otherwise `clientColorTheme[DesignEnv]` |
| `isDarkTheme[]`                           | flag set when `colorTheme[DesignEnv]` of the current design environment equals `ColorTheme.dark` |

`name[ColorTheme]` returns the theme caption; `colorThemeName[DesignEnv]` returns the name of the chosen theme. The form `dialogColorThemes` lists the two themes for selection.

### Computers

| Class / property                  | What it is                                                                     |
|-----------------------------------|---------------------------------------------------------------------------------|
| `Computer`                        | a client computer (inherits from `DesignEnv`)                                   |
| `hostname[Computer]`              | the computer's host name                                                        |
| `currentComputer[]`               | the computer of the current connection                                          |
| `hostnameCurrentComputer[]`       | `hostname[Computer]` of `currentComputer[]`                                     |

The form `computers` lists the computers (with creation, editing, and deletion); `computersDialog` is the selection dialog. The lookup `computer[]` finds the last computer by host name.

### Contacts

`Contact` is an abstract class describing a person or organization that the application keeps contact details for. `CustomUser` inherits from it, so every login-capable user is also a contact.

| Property                  | What it holds                                                          |
|---------------------------|------------------------------------------------------------------------|
| `firstName[Contact]` / `lastName[Contact]` | the first and last name                                |
| `name[Contact]`           | the full name, `firstName[Contact]` and `lastName[Contact]` joined by a space |
| `phone[Contact]`          | the phone number                                                       |
| `postAddress[Contact]`    | the postal address                                                     |
| `birthday[Contact]`       | the date of birth                                                      |
| `email[Contact]`          | the email address, validated against an email pattern                 |
| `attributes[Contact, STRING]` | arbitrary additional attributes keyed by name                      |

The lookup `contact[STRING]` finds a contact by email.

### Users

| Class / property          | What it is                                                                       |
|---------------------------|-----------------------------------------------------------------------------------|
| `User`                    | the abstract base class of all users (inherits from `DesignEnv`)                 |
| `SystemUser`              | the user the platform itself acts as for internal operations                     |
| `CustomUser`              | a regular login-capable user; also a `Contact`                                   |
| `name[User]`              | the user's name: a fixed system caption for a `SystemUser`, the contact name for a `CustomUser` |
| `login[CustomUser]`       | the login name                                                                   |
| `isLocked[CustomUser]`    | flag forbidding the user to sign in                                              |

Lookups: `customUserLogin[ISTRING]` finds a user by login (case-insensitively), `customUserEmail[ISTRING]` finds one by email, `isLockedLogin[ISTRING]` reports whether the user with the given login is locked.

### Passwords and tokens

| Property / action                         | What it does                                                                 |
|-------------------------------------------|------------------------------------------------------------------------------|
| `sha256Password[CustomUser]`              | the SHA-256 hash of the user's password                                      |
| `changeSHA256Password[CustomUser]`        | prompts for a new password and writes its hash to `sha256Password[CustomUser]` |
| `setSHA256Password[CustomUser, STRING]`   | hashes the given password and writes it to `sha256Password[CustomUser]`      |
| `passwordResetToken[CustomUser]` / `expiryPasswordResetTokenDate[CustomUser]` | the one-time password-reset token and its issue time |
| `userByPassResetToken[STRING]`            | finds the user holding the given reset token                                 |
| `code2FA[CustomUser]` / `expiry2FA[CustomUser]` / `lastInitiated2FA[CustomUser]` | the current two-factor code, its expiry, and the time the last code was issued |
| `getAuthToken[]`                          | exports the current request's authentication token (an API action)           |
| `generateAuthToken[User]` / `generateAuthToken[User, INTEGER]` | issues a new authentication token for a user, with an optional expiration in minutes |

`changeSHA256Password[CustomUser]` is wired into the `customUser` form as the on-change handler of `sha256Password[CustomUser]`. The reset tokens older than an hour are cleared by `clearPassResetToken[]`.

### Current user

`currentUser[]` is the user of the current connection. `currentUserName[]` is `name[User]` of that user, and `currentUserLogin[]` is `login[CustomUser]` of that user.

### Authentication providers and settings

The module keeps the configuration for the external authentication mechanisms.

**LDAP**: `useLDAP[]` switches LDAP authentication on; `serverLDAP[]`, `portLDAP[]`, `baseDNLDAP[]`, `userDNSuffixLDAP[]`, `allowOnlyBaseDNUsers[]`, `allowOnlyGroupUsers[]` are the connection and filtering settings. `useDefaultAuthentication[]` allows falling back to the built-in password authentication when LDAP is unavailable, and `disableEmailLoginFallback[]` restricts login lookup to the login only (no email fallback). `useServiceUser[]`, `serviceUser[]`, `serviceUserPassword[]` configure the service account used to read the directory.

**OAuth2 (client role)**: the application delegates sign-in to an external provider (Google, GitHub, and so on). The class `OAuth2` holds one provider configuration — `id[OAuth2]`, `clientId[OAuth2]`, `clientSecret[OAuth2]`, `clientAuthenticationMethod[OAuth2]`, `scope[OAuth2]`, `authorizationUri[OAuth2]`, `tokenUri[OAuth2]`, `jwkSetUri[OAuth2]`, `userInfoUri[OAuth2]`, `userNameAttributeName[OAuth2]`, `clientName[OAuth2]`. The lookup `auth[STRING]` finds a provider by id. The `writeDefaultCredentials[]` action seeds the standard provider entries (GitHub, Google, Facebook, Yandex).

**OAuth authorization server (server role)**: the application itself issues tokens so external apps can call its API on a user's behalf.

| Class / property                              | What it holds                                                            |
|-----------------------------------------------|--------------------------------------------------------------------------|
| `OAuthClient`                                 | a registered external application                                        |
| `clientId[OAuthClient]` / `clientName[OAuthClient]` | the client's identifier and display name                          |
| `redirectURIs[OAuthClient]`                   | the newline-separated allowed redirect URIs                             |
| `trusted[OAuthClient]`                        | flag that skips the consent screen for the client                       |
| `createdAt[OAuthClient]`                      | the registration time                                                   |
| `OAuthRefreshToken`                           | an issued refresh token                                                 |
| `token[OAuthRefreshToken]` / `client[OAuthRefreshToken]` / `user[OAuthRefreshToken]` | the token, the client it was issued to, and the user it represents |
| `expiresAt[OAuthRefreshToken]` / `revokedAt[OAuthRefreshToken]` | the expiry time and the revocation time (empty while valid)  |
| `active[OAuthRefreshToken]`                   | flag set when `expiresAt[OAuthRefreshToken]` is in the future and `revokedAt[OAuthRefreshToken]` is empty |

The lookups `oauthClient[STRING]` and `oauthRefreshToken[STRING]` find a client and a refresh token by their string keys. The token-lifetime tunables `oauthAccessTokenExpiration[]`, `oauthRefreshTokenExpiration[]`, `oauthAuthCodeExpiration[]` (in minutes) override the built-in defaults when set. The form `oauthClient` edits one registered client.

**Web client**: `webClientSecretKey[]` is the shared secret the web client presents to fetch the provider configuration.

### Two-factor authentication settings

`use2FA[]` switches two-factor authentication on. `twoFaRateLimitSeconds[]` limits how often a code can be requested, `twoFaExpirySeconds[]` is how long an issued code stays valid, and `twoFaMaxAttempts[]` caps the verification attempts.

### API actions

These actions are HTTP endpoints (marked with `@@api`); the ones marked `@@noauth` are reachable without prior authentication. They read their input from a `JSONFILE` and write the outcome through `EXPORT FROM`.

| Action                            | What it does                                                                  |
|-----------------------------------|-------------------------------------------------------------------------------|
| `registerUser[JSONFILE]`          | creates a `CustomUser` from a login, password, name, and email, after checking password strength and login uniqueness |
| `resetPassword[JSONFILE]`         | issues a reset token for the user found by login or email and emails the reset link |
| `changePassword[JSONFILE]`        | sets a new password for the user holding the given reset token               |
| `send2FACode[JSONFILE]`           | generates a two-factor code (rate-limited) and emails it to the user         |
| `verify2FACode[JSONFILE]`         | checks a submitted two-factor code against the stored one and its expiry      |
| `getClientCredentials[STRING]`    | returns the OAuth2 provider configuration when the supplied secret matches `webClientSecretKey[]` |
| `getCurrentUserLocale[]`          | exports the current user's `language[CustomUser]` and `country[CustomUser]`   |
| `syncUsers[ISTRING, JSONFILE]`    | reconciles the logins a client has stored, returning the ones that exist and are not locked |
| `getAuthToken[]`                  | exports the current request's authentication token                            |

`onUserRegister[User]` is an abstract list of actions run for each newly registered user, available for extension.

### Password policy and strength

| Property / action                         | What it does                                                                 |
|-------------------------------------------|------------------------------------------------------------------------------|
| `passwordMinLength[]`                     | the minimum password length (in group `policyStrengthPassword`)              |
| `passwordContainsDigits[]` / `passwordContainsSymbols[]` / `passwordContainsUpper[]` | flags requiring digits, special symbols, and uppercase letters |
| `weakPassword[STRING]`                    | the list of unmet requirements for a password, or empty when it complies     |
| `passwordStrength[STRING]`                | a numeric strength score from `0` to `100`                                   |
| `checkPasswordStrength[STRING]`           | an abstract action that writes `passwordStrengthError[]` when the password is too weak |
| `changePasswordOnNextLogin[CustomUser]`   | flag forcing the user to change the password at the next sign-in             |
| `changePassword[CustomUser]` / `changePassword[]` | the interactive password-change dialog for the given (or current) user |

`changePassword[CustomUser]` opens the `changePasswordUser` form, checks the old password against `sha256Password[CustomUser]`, enforces `weakPassword[STRING]` and the confirmation match, and on success writes the new hash and clears `changePasswordOnNextLogin[CustomUser]`.

### Locale settings

A user works in a resolved language, country, time zone, year-start, and date/time format. Each is computed from three layers, with the user's own value taking priority, then the application default, then the server value. The user layer can in turn defer to the values the client reports.

The settings come in parallel groups:

- **User** (`userLanguage[CustomUser]`, `userCountry[CustomUser]`, `userTimeZone[CustomUser]`, `userTwoDigitYearStart[CustomUser]`, `userDateFormat[CustomUser]`, `userTimeFormat[CustomUser]`) — set explicitly for the user.
- **Client** (`clientLanguage[CustomUser]`, `clientCountry[CustomUser]`, `clientTimeZone[CustomUser]`, `clientDateFormat[CustomUser]`, `clientTimeFormat[CustomUser]`) — reported by the user's client, used only when the matching flag is on: `useClientLocale[CustomUser]` for language/country, `useClientTimeZone[CustomUser]` for the time zone, `useClientDateTimeFormat[CustomUser]` for the formats.
- **Default** (`defaultUserLanguage[]`, `defaultUserCountry[]`, `defaultUserTimezone[]`, `defaultUserTwoDigitYearStart[]`, `defaultUserDateFormat[]`, `defaultUserTimeFormat[]`) — the application-wide fallback.
- **Server** (`serverLanguage[]`, `serverCountry[]`, `serverTimezone[]`, `serverTwoDigitYearStart[]`, `serverDateFormat[]`, `serverTimeFormat[]`) — the last-resort fallback.

The resolved values are computed as follows:

| Resolved property        | Resolution order                                                                                  |
|--------------------------|----------------------------------------------------------------------------------------------------|
| `language[CustomUser]`   | client language (when `useClientLocale[CustomUser]`), then user, then default, then server         |
| `country[CustomUser]`    | client country (when `useClientLocale[CustomUser]` and a client language is reported), else the country of the first non-empty layer among user, default, server |
| `timeZone[CustomUser]`   | client time zone (when `useClientTimeZone[CustomUser]`), then user, then default, then server       |
| `dateFormat[CustomUser]` / `timeFormat[CustomUser]` | client format (when `useClientDateTimeFormat[CustomUser]`), then user, then default, then server |
| `twoDigitYearStart[CustomUser]` | user value, then default, then server                                                       |

### Session and user management

| Action                | What it does                                                                              |
|-----------------------|-------------------------------------------------------------------------------------------|
| `logOut[]`            | logs the current user out, optionally restarting and reconnecting the client              |
| `shutdown[]`          | logs out without restart or reconnect                                                     |
| `reconnect[]`         | logs out, then restarts and reconnects                                                    |
| `restart[]`           | logs out and restarts (used by the `Logout` navigator action)                            |
| `relogin[CustomUser]` | re-authenticates the current connection as the given user                                 |

### Object locks

A lock records which user holds an object, so two users do not edit it at once. `locked[Object]` is the holding user. `lock[Object]` takes the lock in a serializable session, succeeding only when the object is not already held; `unlock[Object]` releases it.

### Date-time picker ranges

Each user selects which predefined ranges appear in the single-date and date-interval pickers. `isDateTimeRangeSelected[DateTimePickerRanges, CustomUser]` and `isIntervalRangeSelected[DateTimeIntervalPickerRanges, CustomUser]` are the per-range selection flags; no more than seven of each may be selected. `setDefaultRanges[CustomUser]` turns on the standard starting set, and runs automatically when a `CustomUser` is created. The picker classes themselves come from [`Time`](System_Time.md).

### Forms and navigator

| Form                  | Purpose                                                                  |
|-----------------------|--------------------------------------------------------------------------|
| `customUser`          | the full editing form for one user (login, password, contact, locale, security, picker ranges) |
| `customUsers`         | the administration form for users together with the locale, LDAP, OAuth2, 2FA, web-auth, OAuth-server, and password-policy settings |
| `customUsersDialog`   | the user-selection dialog                                                |
| `editProfile`         | the current user's own profile (login, name, email, language)            |
| `changePasswordUser`  | the password-change dialog                                               |
| `computers`           | the list of computers                                                    |

The navigator gets a `security` folder (under `System`, holding `customUsers` and `computers`) and a top-level `account` folder with the profile editor, the change-password action, and the logout action.

### Language

- [Module header](../language/Module_header.md) — the `MODULE` / `REQUIRE` syntax; `Authentication` is pulled in via `REQUIRE Authentication`.
- [`DATA` operator](../language/DATA_operator.md) — declares the stored properties of the users, contacts, and settings.

### See also

- [`System modules`](System_modules.md) — the general inventory of platform modules.
- [`User classes`](User_classes.md) — what abstract and static classes are, as used by `User`, `Contact`, and `ColorTheme`.
- [`Security policy`](Security_policy.md) — how access rights are assigned to the users declared here.
- [`Security`](System_Security.md) — the separate module for roles and access policies.
- [`Service`](System_Service.md) — administration and service actions.
- [`SystemEvents`](System_SystemEvents.md) — login and connection events.
- [`Time`](System_Time.md) — the picker-range classes and the date/time helpers.
