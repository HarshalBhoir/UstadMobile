
# app-angular  
This modules contains angular web application, based on kotlin multi-platform idea it uses packages delivered from kotlin based modules packaged as NPM packages.  
  
## Prerequisites  
After acquiring a copy of this app, the first thing to do is to install [Node v8](https://nodejs.org/en/download/)  or above (Everything depends on it). After Node installation, install angular CLI.   
Finally,change working directory to app-angular and install all dependencies used in the project since node_modules directory is git ignored.   
  
* Installing Angular CLI  
	```  
	npm install -g @angular/cli  
	```  
* Resolving EACCES Permission errors  
If using Linux or MacOS, you need to change your npm global directory as per [Resolving EACCES permissions errors when installing packages globally](https://docs.npmjs.com/resolving-eacces-permissions-errors-when-installing-packages-globally)   
otherwise you will have file permission errors on build.  
  
## Development  
While you are still in app-angular , you can now build this module. It will create modules dependencies from core source ready to be installed.  
  
* Building and installing  
	```  
	./gradlew app-angular:build  
	```  
  
After that your IDE should see all the module dependencies and should be used like this:  
```  
import entity from 'UstadMobile-lib-database-entities';  
  
//it can be used by specifying full path to the class or object  
i.e  
const contentEntry = new entity.com.ustadmobile.lib.db.entities.ContentEntry();  
```  
* Run app locally

Run with default locale

```
ng serve  or  npm run start
```

Running app locally for a specific locale

```  
npm run start:<locale code> 

//i.e npm run start:en
``` 

This will create an app, to run it use http://localhost:4200/  

## Testing  
To execute end to end test with Protractor use:-
  
```  
./gradlew app-angular:ngTest -Ptestmodule=e2e 

//specify port if needed, otherwise test will use default angulat port i.e 4200
./gradlew app-angular:ngTest -Ptestmodule=e2e -Ptestport=<port number>
```
To execute component tests with Karma use:-
```  
./gradlew app-angular:ngTest -Ptestmodule=<component name>  

i.e
./gradlew app-angular:ngTest -Ptestmodule=home
```
## Deployment  
To create production app, you need to generate JS bundles from angular source. To achieve that run the following command on your terminal.  

Deploy for specific locale
```  
npm run build:<locale code> 
//i.d npm run build:en
```

Deploy for all locales

```  
npm run build-prod 
```
