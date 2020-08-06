@echo off
REM this file dups the enzo jar and makes it a module so I can edit Marvin and debug in my IDE
copy Enzo-0.3.6a.jar Enzo-0.3.6a-forIDE.jar
set module_name=eu.hansolo.enzo
set enzoJar=Enzo-0.3.6a-forIDE.jar
set PATH_TO_JAVAFX="c:\Program Files\Java\javafx-sdk-11.0.2\lib"

@REM - generate the module info file
jdeps --module-path %PATH_TO_JAVAFX% --generate-module-info  . %enzoJar%

@rem compile the modeule info file
javac --class-path %CD% --module-path %PATH_TO_JAVAFX% --patch-module %module_name%="%enzoJar%" %module_name%/module-info.java 

@rem add the module info file to the jar file
jar uf %enzoJar% -C %module_name% module-info.class