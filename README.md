# Wurst Client v7

## Downloads (for users)

https://www.wurstclient.net/download/

## Setup (for developers) (using Windows 10 & Eclipse)

Requirements: [JDK 17](https://adoptium.net/?variant=openjdk17&jvmVariant=hotspot)

1. Run these two commands in PowerShell:

```
./gradlew.bat genSources
./gradlew.bat eclipse
```

2. In Eclipse, go to `Import...` > `Existing Projects into Workspace` and select this project.

## Translations

I am experimenting with using Crowdin for translations. For now, you can submit translations either [on Crowdin](https://crowdin.com/project/wurst7) or here on GitHub.

The translation files are located [here](https://github.com/Wurst-Imperium/Wurst7/tree/master/src/main/resources/assets/wurst/lang) and work the same as in other Minecraft mods.

Names of features (hacks/commands/etc.) should always be kept in English. This ensures that everyone can use the same commands, keybinds, etc. regardless of their language setting. It also makes it easier to communicate with someone who uses Wurst in a different language.

For discussion about translations, see [Issue #404](https://github.com/Wurst-Imperium/Wurst7/issues/404) here on GitHub, or [#wurst-translations](https://chat.wurstimperium.net/channel/wurst-translations) on our RocketChat server, or the comments section on [Crowdin](https://crowdin.com/project/wurst7).

## License

This code is licensed under the GNU General Public License v3. **You can only use this code in open-source clients that you release under the same license! Using it in closed-source/proprietary clients is not allowed!**

## Note about Pull Requests

If you are contributing multiple unrelated features, please create a separate pull request for each feature. Squeezing everything into one giant pull request makes it very difficult for us to add your features, as we have to test, validate and add them one by one.

Thank you for your understanding - and thanks again for taking the time to contribute!!
