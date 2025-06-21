# How to use these files

## In Eclipse

Right-click on the project and select `Properties` > `Java Code Style`.

Under `Clean Up`:
1. Enable project specific settings
2. Import the `cleanup.xml` file
3. Select the `Wurst-Imperium` profile
4. Click `Apply`

![screenshot of Eclipse Clean Up settings](https://img.wimods.net/github.com/Wurst-Imperium/Wurst7/codestyle?to=https://i.imgur.com/mHKDHvV.png)

Under `Code Templates`:
1. Enable project specific settings
2. Import the `templates.xml` file
3. Click `Apply`

![screenshot of Eclipse Code Templates settings](https://i.imgur.com/C2ciKnM.png)

Under `Formatter`:
1. Enable project specific settings
2. Import the `formatter.xml` file
3. Select the `Wurst-Imperium` profile
4. Click `Apply`

![screenshot of Eclipse Formatter settings](https://i.imgur.com/cj57gh9.png)

## In VSCode

In the VSCode settings, set `java.format.settings.url` to:

```
https://raw.githubusercontent.com/Wurst-Imperium/Wurst7/master/codestyle/formatter.xml
```

and set `java.format.settings.profile` to:

```
Wurst-Imperium
```

![screenshot of VSCode java.format settings](https://i.imgur.com/9W1s5Gj.png)

## Through Gradle

To check if your code is formatted correctly, run:

```pwsh
./gradlew check
```

It will say `BUILD SUCCESSFUL` if your code is formatted correctly or `BUILD FAILED` if it's not.

To format your code, run:

```pwsh
./gradlew spotlessApply
```

If you want to both check the code style and run the automated tests, run:

```pwsh
./gradlew check runEndToEndTest --warning-mode fail
```

This will take about a minute to run.
