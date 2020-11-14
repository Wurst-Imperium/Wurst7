# Wurst Client v7

## Downloads (for users)

https://www.wurstclient.net/download/

## Setup (for developers) (using Windows 10 & Eclipse)

1. Run these two commands in PowerShell:

```
./gradlew.bat genSources
./gradlew.bat eclipse
```

2. In Eclipse, go to `Import...` > `Existing Projects into Workspace` and select this project.

# Setup (for developers) (using Windows 10 & IntelliJ IDEA)

1. In IntelliJ IDEA, click `Import Project`, press the refresh button on the top, and go to where you placed this project at, open the folder and select `build.gradle`.

2. Wait for the import to finish.

3. To run, click on `Add Configuration...`, expand `Applications` in the left tab of the window that appears, click on `Minecraft Client`, press `Ok` and run.

## License

This code is licensed under the GNU General Public License v3. **You can only use this code in open-source clients that you release under the same license! Using it in closed-source/proprietary clients is not allowed!**

## Note about Pull Requests

If you are contributing multiple unrelated features, please create a separate pull request for each feature. Squeezing everything into one giant pull request makes it very difficult for us to add your features, as we have to test, validate and add them one by one.

Thank you for your understanding - and thanks again for taking the time to contribute!!
