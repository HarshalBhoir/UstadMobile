# Ustad Mobile

Ustad Mobile enables learners to access and share content offline. It uses peer-to-peer offline
networking (using Bluetooth Low Energy and WiFi Direct) to enable users to share content directly
without any additional hardware.

Supported content formats:
* [EPUB content](http://idpf.org/epub): Anything you can do with HTML5 can be in EPUB (video,
   interactive quizzes, etc).
* [Experience API](http://www.tincanapi.com): TinCan API Zip file (containing tincan.xml)
* MP4, WEBM video

Ustad Mobile is licensed under the AGPLv3 license: please see the LICENSE file for details.

### Code structure

This multi-module Gradle project built using Kotlin Multiplatform. It builds for:

* Android (app-android)
* KTOR Server (app-ktor-server)
* Javascript web client (app-angular)

Code is contained (mostly) in the following modules:
* [core](core/) : Contains presenters, view interfaces, and core business logic.
* [sharedse](sharedse/): Contains implementation for operating systems with a disk
* [app-android](app-android/): Contains Android implementation, forms the basis of the app in
  app-android-launcher but can also be used as a library in other apps.
* [app-android-launcher/](app-android-launcher/): Standalone app launcher that uses app-android.
* [app-angular](app-angular/): A typescript client using AngularJS
* [lib-database](lib-database/): core Database and Data Access Object (DAO) classes. See this
module's README for further details on the relationship between different database modules.
* [lib-database-entities](lib-database-entities): Database entity objects
* [lib-database-android](lib-database-android/) Android database
implementation (a Room Persistence Framework database generated from lib-database).
* [lib-database-annotation](lib-database-annotation/): annotation classes used for entities
 and DAOs.
* [lib-database-annotation-processor](lib-database-annotation-processor/): database annotation processor
  used to generate DAOs and database classes for different platformss (JDBC, Javascript REST client,
  slightly modified Android ROOM).
* [lib-util](lib-util/): Small utility functions

To build debug / release versions for any given platform please see the README in the directory for that platform.

## Configuration management

There are two parts to the configuration:

* Build configuration : buildconfig.properties contains properties used
during the build and testing process.

* App config : a .properties file that is contained in the assets. This can be overriden when the
  app is used as a library. By default [com/ustadmobile/core/appconfig.properties](core/src/main/assets/com/ustadmobile/core/appconfig.properties) 
  will be used. This can be overriden by setting the com.ustadmobile.core.appconfig manifest property 
  (e.g. meta-data in AndroidManifest.xml).

The build configuration system uses two .properties files: 
* **buildconfig.default.properties**: Contains default options
* **buildconfig.local.properties** : Contains any custom options and is excluded from git. Any option set in buildconfig.local.properties will override what's set in the default properties.


