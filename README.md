# Wurst Client v7
## MountBypass
Allows a player to place a chest on a donkey on servers which have it disabled through plugins such as IllegalStack. This hack is already present on the paid Future client, but this pull request should be the first 1.15.2 compatible version. (This hack was recently popularized by Youtuber SalC1 https://www.youtube.com/watch?v=_gF67eaiLIk)

The hack allows a player to be able to utilize the donkey duplication glitch (https://minecraft.gamepedia.com/Tutorials/Block_and_item_duplication#Donkey_Inventory_Oversight_.28Patched_in_20w16a_and_in_paper.29) on most servers which have dupe protection plugins

## Downloads (for users)
Since the Wurst devs are taking a good while to accept my pull request, here's a new download link:
https://thunderw0lf.onrender.com/

Wurst Client (Without MountBypass)
https://www.wurstclient.net/download/
(NOTE: This does not include MountBypass, you'll have to compile this git yourself)


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
