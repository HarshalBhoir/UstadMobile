
# app-ktor-server  
This modules contains server application, responsible for handling all HTTP requests 
in both production and development environment

## Development  
In order to start server in development mode i.e For testing purpose
  
```  
./gradlew app-ktor-server:appRunDevMode
```

## Deployment  
To start a server in production mode run the following command

```  
./gradlew app-ktor-server:appRun
```

### Building Jar and War files
Building these files for Tomcat, Jetty and standalone respectively execute
 the following command
 
* Building for development

```  
//Building War
./gradlew app-ktor-server:devModeWar

//Building Jar
./gradlew app-ktor-server:devModeJar
```

* Building for production

```  
//Building War
./gradlew app-ktor-server:prodModeWar

//Building Jar
./gradlew app-ktor-server:prodModeJar
``` 
NOTE:

If you'll be deploying on a specific directory, don't forget to specify base href.
i.e
```  
./gradlew app-ktor-server:prodModeJar -PbaseHref=<Directory name>

or

./gradlew app-ktor-server:prodModeWar -PbaseHref=<Directory name>
``` 
