# Wurst Client v7

## Downloads (for users)

[https://www.wurstclient.net/download/](https://www.wurstclient.net/download/?utm_source=GitHub&utm_medium=Wurst7&utm_campaign=README.md&utm_content=Downloads+%28for+users%29)

## Setup (for developers)

(This assumes that you are using Windows with [Eclipse](https://www.eclipse.org/downloads/) and [Java Development Kit 8](https://adoptium.net/?variant=openjdk8&jvmVariant=hotspot) already installed.)

1. Run these two commands in PowerShell:

```
./gradlew.bat genSources
./gradlew.bat eclipse
```

2. In Eclipse, go to `Import...` > `Existing Projects into Workspace` and select this project.

## Contributing

If you want to help but are not sure what to do, take a look at our [planning board](https://github.com/orgs/Wurst-Imperium/projects/5/views/1) or the [help wanted list](https://github.com/Wurst-Imperium/Wurst7/issues?q=is%3Aissue+is%3Aopen+label%3A%22help+wanted%22). Of course you can contribute anything you like, but these issues are particularly useful.

If you are contributing multiple unrelated features, please create a separate pull request for each feature. Squeezing everything into one giant pull request makes it very difficult for me to add your features, as I have to test, validate and add them one by one.

Thank you for your understanding - and thanks again for taking the time to contribute!!

## Translations

We have a [Crowdin project](https://crowdin.com/project/wurst7) for translations. You can also submit translations here on GitHub, but Crowdin is preferred since it makes it much easier to resolve issues.

To enable translations in-game, go to Wurst Options > Translations > ON.

Names of features (hacks/commands/etc.) should always be kept in English. This ensures that everyone can use the same commands, keybinds, etc. regardless of their language setting. It also makes it easier to communicate with someone who uses Wurst in a different language.

The translation files are located in [this folder](https://github.com/Wurst-Imperium/Wurst7/tree/master/src/main/resources/assets/wurst/lang), in case you need them.

## License

This code is licensed under the GNU General Public License v3. **You can only use this code in open-source clients that you release under the same license! Using it in closed-source/proprietary clients is not allowed!**
