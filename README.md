# Wurst Client v7

## ⚠ We Are Looking For Translators ⚠

### Current Status of Translations

|Language|Status|
|--------|--------|
|Chinese (Simplified/Mainland)|147/147 done|
|Chinese (Traditional/Taiwan)|147/147 done|
|Chinese (Cantonese/Hong Kong)|Pending, needs reviews, check [#555](https://github.com/Wurst-Imperium/Wurst7/pull/555).|
|French (France)|There are 3 pending submissions ([#515](https://github.com/Wurst-Imperium/Wurst7/pull/515), [#531](https://github.com/Wurst-Imperium/Wurst7/pull/531), [#552](https://github.com/Wurst-Imperium/Wurst7/pull/552)) with different translations and I don't know which one to merge. If you speak French, please check these submissions for any grammatical errors and let me know which one sounds best to a native speaker.|
|German (Germany)|46/147 done. I'll probably do the rest myself since I can speak it natively.|
|Italian (Italy)|146/147 done|
|Japanese (Japan)|137/147 done|
|Polish (Poland)|147/147 done|
|Portugese (Brazil)|Pending, needs reviews, check [#528](https://github.com/Wurst-Imperium/Wurst7/pull/528).|
|Russian (Russia)|137/147 done|
|Turkish (Turkey)|There are 2 pending submissions ([#511](https://github.com/Wurst-Imperium/Wurst7/pull/511), [#512](https://github.com/Wurst-Imperium/Wurst7/pull/512)) with different translations and I don't know which one to merge. If you speak Turkish, please check these submissions for any grammatical errors and let me know which one sounds best to a native speaker. |
|Ukrainian (Ukraine)|137/147 done|

If you speak both English and some other language, please help us by translating Wurst or reviewing existing translations. The translation files are located [here](https://github.com/Wurst-Imperium/Wurst7/tree/master/src/main/resources/assets/wurst/lang) and work the same as in other Minecraft mods.

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
