# Wurst Client v7

## Downloads (for users)

[https://www.wurstclient.net/download/](https://www.wurstclient.net/download/?utm_source=GitHub&utm_medium=Wurst7&utm_campaign=README.md&utm_content=Downloads+%28for+users%29)

## Setup (for developers)

(This assumes that you are using Windows with [Eclipse](https://www.eclipse.org/downloads/) and [Java Development Kit 17](https://adoptium.net/?variant=openjdk17&jvmVariant=hotspot) already installed.)

1. Run this command in PowerShell:

```
./gradlew.bat genSources eclipse --no-daemon
```

2. In Eclipse, go to `Import...` > `Existing Projects into Workspace` and select this project.

## Contributing

Pull requests are welcome, but please make sure to read the [contributing guidelines](CONTRIBUTING.md) first.

## Translations

We have a [Crowdin project](https://crowdin.com/project/wurst7) for translations. You can also submit translations here on GitHub, but Crowdin is preferred since it makes it much easier to resolve issues.

To enable translations in-game, go to Wurst Options > Translations > ON.

Names of features (hacks/commands/etc.) should always be kept in English. This ensures that everyone can use the same commands, keybinds, etc. regardless of their language setting. It also makes it easier to communicate with someone who uses Wurst in a different language.

The translation files are located in [this folder](https://github.com/Wurst-Imperium/Wurst7/tree/master/src/main/resources/assets/wurst/lang), in case you need them.

## License

This code is licensed under the GNU General Public License v3. **You can only use this code in open-source clients that you release under the same license! Using it in closed-source/proprietary clients is not allowed!**
