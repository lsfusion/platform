<p align="center">
  <span>English</span> |
  <a href="https://github.com/lsfusion/platform/tree/master/README_ru.md#lsfusion-">Russian</a>
</p>

# lsFusion <a href="http://lsfusion.org" target="_blank"><img src="https://lsfusion.org/imgs/logo.svg" align="right"/></a>  

lsFusion is a free open-source platform for information systems development based on the fifth-generation programming language of the same name.

Incremental computing, function-level, reactive, and event-based programming, the ability to write and iterate over all function values, multiple inheritance and polymorphism, no encapsulation, and lots of other fundamentally new ideas greatly improve the development speed, quality, and performance of created systems compared to traditional approaches.

[comment]: <> (<p align="center">)
[comment]: <> (<a href="#main-features">Main Features</a> •)
[comment]: <> (  <a href="#installation">Installation</a> •)
[comment]: <> (  <a href="#try-online">Try online</a> •)
[comment]: <> (  <a href="#code-examples">Code examples</a> •)
[comment]: <> (  <a href="#online-demos">Online demos</a> •)
[comment]: <> (  <a href="#links">Links</a> •)
[comment]: <> (  <a href="#feedback">Feedback</a> •)
[comment]: <> (  <a href="#license">License</a>)
[comment]: <> (</p>)

[comment]: <> (---)

## Main features
- **Single language for data**

  The platform is free from [the object-relational semantic gap](https://en.wikipedia.org/wiki/Object-relational_impedance_mismatch), so developers don't need to constantly choose between "rapid" SQL queries and a "convenient" imperative language. Both of these approaches are almost fully abstracted and united. Each developer always works with data using a single paradigm, while the platform takes care of how and where to do all the remaining work.

- **No ORM, Yes SQL**

  When a certain action requires data processing for many objects at once, the platform tries, whenever possible, to perform this processing on the database server through a single request (i.e. for all objects at once). In this case, all of the queries generated are optimized as much as possible depending on the features of the database server.

- **Absolute reactivity**

  All computed data is automatically updated once the data it uses is changed. This rule applies always and everywhere, whether you're displaying interactive forms or simply accessing data inside the action being executed.

- **Dynamic physical model**

  The platform allows you to materialize any existing indicators in the system at any time, add new tables or delete existing ones, or change the location of materialized indicators in these tables.

- **Constraints on any data**

  The platform allows you to create constraints for the values of any data (even computed data), and all such constraints (as with events) are global, so inexperienced users or developers cannot bypass them with an invalid transaction.

- **Efficient client-server communication**

  Client-server communication at the physical level minimizes synchronous round-trip calls (i.e. each user action leads to a single - usually asynchronous - request/response pair). The desktop client also archives and encrypts all transferred data (when necessary). During client-server communication (via TCP/IP for the desktop client or HTTP for the Web client), the platform provides guarantee of delivery - it resends any lost requests and ensures that they are processed in the correct order. All these features help the platform run efficiently even on low-bandwidth, unstable, or high-latency communication channels.

- **Three-tier architecture**

  The platform executes the imperative part of the system logic (i.e. everything related to data changes) on application servers and the declarative part (i.e. everything related to data calculations) on database servers. This separation simplifies the scaling of the system you develop and strengthens its fault tolerance due to the different nature of workload on these servers (e.g. using swap on an application server is much more dangerous than on a database server).

- **Everything as code**

  All elements of the system, from events to form design, are written in the lsFusion language and stored in ordinary text files (without any shared repositories with an unknown structure). This allows you to use popular version control systems (Git, Subversion) and project-building tools (Maven in IDEs) when working with projects. In addition, this approach simplifies the support and deployment of the system you develop - you can use an ordinary text editor to view and quickly modify the logic when necessary and also easily identify any element in the system by file name and line number in this file.

- **lsFusion programming language**

  - Polymorphism and aggregations

      Supports inheritance (including multiple inheritance) and polymorphism (including multiple polymorphism). And, if inheritance isn't enough for you for whatever reason, the platform also provides an aggregation mechanism that, together with inheritance, allows you to implement almost any polymorphic logic.

  - Modules and extensions

      The extension technique allows developers to extend the functionality of one module in another (e.g. they can modify forms or classes created in another module). This mechanism makes the solutions you create much more modular.

  - Metaprogramming

      If you want to create your own high-level operator, or maybe you just don't know how to generalize the logic, but want to reuse it, lsFusion provides full support for automatic code generation, from both the server side and IDE.

  - Namespaces
  - Java & SQL integration
  - Internationalization

- **Easy-to-use IDE**

  Intellij IDEA-based IDE with everything developers could ever need: search for uses, code/error highlighting, smart auto-completion, quick jump to declarations, class structure, renaming of elements, usage tree, data change breakpoints, debuggers, and more.

- **Advanced tools for administrators**

  The platform provides a complete set of administration tools for systems that are already running: interpreter (executes lsFusion code), process monitor (receives detailed information about processes that are running, e.g. call start time, stack, user, etc.), scheduler (executes periodic or scheduled actions), profiler (measures the performance of all actions executed for all/given users, e.g. builds a graph of calls, sharing time between the application server and the database server, etc.), messenger (for internal notifications/communication with users in the system), and numerous logs (connections, errors, etc.).

For a more complete list see [lsfusion website](https://lsfusion.org/opportunities).

## Installation

See https://docs.lsfusion.org/Install/

## Code examples
- [Score table](https://docs.lsfusion.org/Score_table/)

  Simple application that allows you to calculate the score table of a hockey tournament. It contains exactly one form, in which the user can enter game scores, based on which the score table is automatically built. 
  
  Using this example you can get an idea of how to quickly develop "Excel-style" applications in which form data are editable, and any changes to them will cause all dependent data on the form to be updated incrementally.

- [Materials management](https://docs.lsfusion.org/Materials_management/)

  Example of creating a simple stock management business application. In it, the user can manage receipt and shipment operations, and also obtain item balances.

  This example shows a way to create an application for processing documents that have headers and lines. All forms are created in "Dialog style". In this approach, for each class in the system, a form with their list is created, in which only buttons for creating, editing and deleting objects are available for editing. Clicking the corresponding button opens a separate dialog form, with which the user can create a new object or edit an existing one.

- [How-To](https://docs.lsfusion.org/How-to/)

  How-to section of documentation contains examples of typical tasks, categorized.

## Try online
It is possible to run the code snippets in lsfusion programming language [online](https://lsfusion.org/try).

## Online demos
- [Score table](https://demo.lsfusion.org/hockeystats) 
  - [docs](https://docs.lsfusion.org/Score_table/) 
  - [github](https://github.com/lsfusion/samples/tree/master/hockeystats) 
  - username: guest
  - password: guest 

- [Materials management (MM)](https://demo.lsfusion.org/mm)
  - [docs](https://docs.lsfusion.org/Materials_management/)
  - [github](https://github.com/lsfusion/samples/tree/master/mm) 
  - username: guest
  - password: guest 

## Links
- [Homepage](https://lsfusion.org)
- [Documentation](https://docs.lsfusion.org/)
- [Repository](https://github.com/lsfusion/platform)
- [Downloads](https://download.lsfusion.org/)

## Feedback
- [Issue tracker](https://github.com/lsfusion/platform/issues) 
- [Slack community](https://slack.lsfusion.org)
- [Telegram group](https://t.me/lsfusion_official)

## License
The platform is licensed under [LGPL v3](http://www.gnu.org/licenses/lgpl-3.0.en.html), which allows you to freely use, distribute, and modify the platform as you wish.
