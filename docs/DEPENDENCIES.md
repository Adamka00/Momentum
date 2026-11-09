# Dependency verification

Verified against official endpoints on **2026-11-09** before implementation.

| Choice | Evidence |
| --- | --- |
| Minecraft 26.3 is a stable release | [Mojang manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json), `latest.release = 26.3`, release time 2026-09-15; [26.3 metadata](https://piston-meta.mojang.com/v1/packages/80608ff6753705b6a82e2cb86a5937d4d233abd4/26.3.json) |
| Java 25 | 26.3 metadata `javaVersion.majorVersion = 25`; [Fabric setup documentation](https://docs.fabricmc.net/develop/getting-started/setting-up) |
| Loader 0.20.1 | [Fabric Loader metadata for 26.3](https://meta.fabricmc.net/v2/versions/loader/26.3/0.20.1); stable Loader entry |
| API 0.166.0+26.3 | [Published Fabric API versions](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml), [exact POM](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.166.0+26.3/fabric-api-0.166.0+26.3.pom) |
| Loom 1.19.4 | [Exact published module](https://maven.fabricmc.net/net/fabricmc/fabric-loom/1.19.4/fabric-loom-1.19.4.module); requires Java 25 and Gradle plugin API 9.8.0 |
| Gradle 9.8.2 | [Official 26.3 example wrapper](https://github.com/FabricMC/fabric-example-mod/blob/26.3/gradle/wrapper/gradle-wrapper.properties), [distribution SHA-256](https://services.gradle.org/distributions/gradle-9.8.2-bin.zip.sha256) |
| No mappings artifact | [Official example build](https://github.com/FabricMC/fabric-example-mod/blob/26.3/build.gradle) and [Fabric mapping migration explanation](https://docs.fabricmc.net/develop/porting/mappings/) |

The official 26.3 template used a Loom 1.19 snapshot and API 0.165.2+26.3 when inspected. This project pins the published stable Loom 1.19.4 and newer verified API 0.166.0+26.3 instead. All selected artifacts were resolved and compiled, rather than inferred from a 1.21.x setup.

Fabric's web documentation displayed **26.2** for the input, networking, and HUD guides when inspected. Those pages provided architecture guidance; exact 26.3 signatures were checked in the downloaded Minecraft JAR and matching Fabric API classes/sources, then compiled. In particular:

- `InputConstants.Type.KEYBOARD` replaces the older `KEYSYM` name shown by the web guide.
- Screens are accessed through `Minecraft.gui.screen()`.
- `LocalPlayer.input.keyPresses` is the immutable `Input` record (`forward()`, `backward()`, `left()`, `right()`, `jump()`); the mod does not use legacy impulse fields.
- Payload registration uses `PayloadTypeRegistry.serverboundPlay()` / `clientboundPlay()` and `StreamCodec<RegistryFriendlyByteBuf, T>`.
- The HUD uses `HudElementRegistry` and `GuiGraphicsExtractor.text()`.
- Environment source sets follow the official `splitEnvironmentSourceSets()` pattern with `implementation` dependencies for these unobfuscated versions.

Other official guides consulted: [networking](https://docs.fabricmc.net/develop/networking), [key mappings](https://docs.fabricmc.net/develop/key-mappings), [HUD](https://docs.fabricmc.net/develop/rendering/hud), and [automated testing](https://docs.fabricmc.net/develop/automatic-testing).

Build host: Linux x86_64, Temurin 25.0.5+8. Downloaded toolchain SHA-256 was verified against Adoptium metadata. macOS ARM64 execution is a separate runtime check; no Linux native binaries or host-specific paths are embedded in the project.
