# artemis
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
