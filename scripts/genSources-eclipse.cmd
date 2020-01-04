@echo off
cd ..
call gradlew.bat genSources
call gradlew.bat eclipse
pause