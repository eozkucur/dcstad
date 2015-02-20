##TAD for DCS


###Overview

This is a simple Tactical Awareness Display (TAD) for Digital Combat Simulator (DCS) F-15C and possibly other modules. It includes export scripts, a PC component for displaying TAD view (and function as a server) and an android app. The android app has a discovery mechanism and no IP configuration is required. 

The instructions below are for building the project from source. For convenience, the compiled binaries can be downloaded from [DCS website](http://www.digitalcombatsimulator.com/en/files/1277731/).

###Requirements

* Download the latest release from the [releases](https://github.com/eozkucur/dcstad/releases) page.
* Install latest Java Development Kit (JDK) [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* Internet connection. Other required packages will be downloaded automatically during build.
* (Optional) If you want to compile and use the Android app, install Android SDK.

###Installation From Source

* Open a command prompt `cmd.exe` and verify the JDK installation with command

  ```
  javac -version
  ```
* If you have no existing export script, copy `Export.lua`, `dcstad.lua` files and `MessagePack` folder from `dcstad\lua` folder to `C:\<user>\Documents\Saved Games\DCS\Scripts` folder.

* If you have an existing export script, copy `dcstad.lua` file and `MessagePack` folder from `dcstad\lua` folder to `C:\<user>\Documents\Saved Games\DCS\Scripts` folder. And copy single line content from `dcstad\lua\Export.lua` to the end of your existing `Export.lua` file.

  ####Only PC application
  
  * Modify `dcstad\settings.gradle` file and remove `, ':androidapp'` part.
  * Build the project with commands
    
    ```
    cd <dcstad directory>\dcstad
    gradlew.bat dcsserver:jar
    ```
    
    First build will take a while.
    
  * The output is a single jar without and dependency and it is located in `dcstad\dcsserver\build\libs\dcsserver.jar`
  
  ####PC and Android application
  
  * Modify `dcstad\local.properties` file and enter your Android SDK path.
  * Build the project with commands
  
    ```
    cd <dcstad directory>\dcstad
    gradlew.bat assemble
    ```
  
  * The output of the PC application is a single jar without any dependency and it is located in `dcstad\dcsserver\build\libs\dcsserver.jar`
  * The generated android package is located in `dcstad\androidapp\build\outputs\apk\androidapp-debug.apk`
  
###Usage

* If your JDK installation is configured properly, you can double click on the jar file.
* If double clicking does not work, you can run the jar with command
  ```
  java -jar dcsserver.jar
  ```
* The application launches into system tray. Right click and chose `Start TAD`.
* For TAD view on PC, right click and chose `Start TAD`.
* For TAD view on Android, right click and chose `Start Server`.
* You can run both android and PC TAD view at the same time.
* `Start Server` option is only required for the android app.
* The TAD view is a borderless always-on-top window. You can drag it with `left click`, and resize it with `right click & drag`.
* `Left click` changes scale.
* `Right click` changes the focused object.
* Due to some issues with the DCS api, the whole route information cannot be retrieved. Instead, cycle waypoints once in the beginning of the simulation.
* The scale is displayed on the top-right corner. The red color indicates no connection and green indicates connected.
* In the android app, single tap changes scale and double tap changes the focused object.

###Versions

#####0.2.2
* Easier integration with existing export scripts.
* Right-click drag to resize window.

#####0.2.1
* Remove destroyed aircraft from the view.
* Added exception handling to export script.

#####0.2.0
* Added allied flights to the TAD display

#####0.1.0
* Initial version
