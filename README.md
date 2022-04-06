# Wurst Client v7

## ⚠ We Are Looking For Translators & Proofreaders ⚠

Mostly proofreaders. Many translations are currently stuck because we have no one who can proofread them.

### Current Status of Translations

- [Check the current status of translations here.](https://github.com/orgs/Wurst-Imperium/projects/4/views/1)

- [Check which translations need proofreaders here.](https://github.com/orgs/Wurst-Imperium/projects/4/views/3)

The translation files are located [here](https://github.com/Wurst-Imperium/Wurst7/tree/master/src/main/resources/assets/wurst/lang) and work the same as in other Minecraft mods.

You don't need permission to become a translator or proofreader. You just need a free GitHub account and then you can submit translations ("pull requests") or proofreading ("reviews").

Names of features (hacks/commands/etc.) should always be kept in English. This ensures that everyone can use the same commands, keybinds, etc. regardless of their language setting. It also makes it easier to communicate with someone who uses Wurst in a different language.

For discussion about translations, see [Issue #404](https://github.com/Wurst-Imperium/Wurst7/issues/404) here or [#wurst-translations](https://chat.wurstimperium.net/channel/wurst-translations) on our RocketChat server.

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

## License

This code is licensed under the GNU General Public License v3. **You can only use this code in open-source clients that you release under the same license! Using it in closed-source/proprietary clients is not allowed!**

## Note about Pull Requests

If you are contributing multiple unrelated features, please create a separate pull request for each feature. Squeezing everything into one giant pull request makes it very difficult for us to add your features, as we have to test, validate and add them one by one.

Thank you for your understanding - and thanks again for taking the time to contribute!!
