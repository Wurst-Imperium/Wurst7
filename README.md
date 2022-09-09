# Wurst Client v7

## ⚠ We Are Looking For Translators ⚠

If you speak both English and some other language, please help us by translating Wurst. The translation files are located in `src/main/resources/assets/wurst/lang` and work the same as in other Minecraft mods.

At the moment, only hack descriptions can be translated. Other descriptions and tooltips will become translatable in the future.

Names of features (hacks/commands/etc.) should always be kept in English. This ensures that everyone can use the same commands, keybinds, etc. regardless of their language setting. It also makes it easier to communicate with someone who uses Wurst in a different language.

See [Issue #404](https://github.com/Wurst-Imperium/Wurst7/issues/404) for discussion about translations.

## Downloads (for users)

https://www.wurstclient.net/download/

## Setup (for developers) (using Windows 10 & Eclipse)

1. Run these two commands in PowerShell:

```
./gradlew.bat genSources
./gradlew.bat eclipse
```

2. In Eclipse, go to `Import...` > `Existing Projects into Workspace` and select this project.

## License

This code is licensed under the GNU General Public License v3. **You can only use this code in open-source clients that you release under the same license! Using it in closed-source/proprietary clients is not allowed!**

## Note about Pull Requests

If you are contributing multiple unrelated features, please create a separate pull request for each feature. Squeezing everything into one giant pull request makes it very difficult for us to add your features, as we have to test, validate and add them one by one.

Thank you for your understanding - and thanks again for taking the time to contribute!!
