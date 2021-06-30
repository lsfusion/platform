---
title: 'Internationalization'
---

*Internationalization* consists of software development techniques that allow adapting the software to the linguistic and cultural particularities of different countries and regions.

### Locale  {#locale}

*Locale* is a set of parameters that defines regional settings, such as:

-   Language
-   Country (which in turn determines the formats for displaying numbers and dates)
-   Timezone
-   The starting year for a 100-year period, if the year is specified by a two-digit number

### Formats for specifying locale parameters

The language and country are specified in [standard Java](https://docs.oracle.com/javase/tutorial/i18n/locale/create.html) format. For example, the language can be specified using the lines `ru`, `en`, and `fr`, and the country, respectively, using `RU`, `US`, and `CA`. The timezone is specified in the format supported by the Java method [`TimeZone.getTimeZone`](https://docs.oracle.com/javase/8/docs/api/java/util/TimeZone.html#getTimeZone-java.lang.String-) (examples: `PST`, `Europe/Minsk`, `GMT-8:00`).  The year is specified as a number (example: `1960`).

### Determining the current locale {#current}

The server locale is determined by the [default locale](http://www.oracle.com/us/technologies/java/locale-140624.html) of the JVM in which the server starts (namely, the startup parameters [user.language, user.country, user.timezone, and user.twoDigitYearStart](Launch_parameters.md#locale)). When the application server starts, the parameters of this locale are automatically saved in the following properties:

| Locale parameter                     | Start parameter          | Property                                   |
| ------------------------------------ | ------------------------ | ------------------------------------------ |
| Language                             | `user.language`          | `Authentication.serverLanguage[]`          |
| Country                              | `user.country`           | `Authentication.serverCountry[]`           |
| Timezone                             | `user.timezone`          | `Authentication.serverTimezone[]`          |
| Starting year of the 100-year period | `user.twoDigitYearStart` | `Authentication.serverTwoDigitYearStart[]` |

The server locale is used as *current* when the action in which localization is being performed is initiated by the system, and not by a particular user (i.e., the system user is considered the current user).

Otherwise, the current locale is determined by the values of the following properties (the user who initiated the action is passed as a parameter):

| Parameter                            | Property                                       |
| ------------------------------------ | ---------------------------------------------- |
| Language                             | `Authentication.language[CustomUser]`          |
| Country                              | `Authentication.country[CustomUser]`           |
| Timezone                             | `Authentication.timezone[CustomUser]`          |
| Starting year of the 100-year period | `Authentication.twoDigitYearStart[CustomUser]` |

In the current platform implementation, the above properties allow you both to use the locale parameters of the user's operating system and to set these parameters explicitly for specific users; or, for example, to use the server locale for all users (this is the default behavior).

### String data localization

The main task of platform internationalization is to localize the string data that the user sees. When sending text messages, property captions, actions, forms, etc. to the client from the server, these can be translated into another language or otherwise converted depending on the [current locale](#current).

Localizable strings are created as follows: in the string, in place of the text to be localized, *the string data ID* is specified in curly brackets (e.g., `'{button.cancel}'`). When this string is sent to the client, all IDs found in the string are searched for on the server, then each is searched for in all [ResourceBundle](https://en.wikipedia.org/wiki/Java_resource_bundle) project files in the required locale. If found, the ID in the brackets is replaced with the corresponding text.
