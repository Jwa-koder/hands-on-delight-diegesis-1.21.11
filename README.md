# Hands-On Delight and Diegesis — unofficial Fabric 1.21.11 ports

Experimental Minecraft Java **1.21.11 / Fabric** ports of [Pocky's Diegesis](https://github.com/Pocky-l/diegesis) and [Hands-On Delight](https://github.com/Pocky-l/handson-delight). These ports are independent of the original author and are not official releases.

Hands-On Delight turns Farmer's Delight's cooking stations into interactive in-world interfaces. Diegesis is the library it uses.

## Install

Download **both JARs** from this repository's [Releases](../../releases) page. Install them on both the client and Fabric server, with:

- Minecraft **1.21.11**, Java **21**; tested with Fabric Loader **0.20.1**.
- [Fabric API **0.141.6+1.21.11**](https://modrinth.com/mod/fabric-api).
- [Farmer's Delight Refabricated **1.21.11-3.6.16**](https://modrinth.com/mod/farmers-delight-refabricated).

Use a fresh test instance/world first. Replace older copies of these mods rather than installing duplicates. This repository does not bundle Minecraft, Farmer's Delight, Fabric API, Weather Glass or AutoModpack.

Right-click a station to focus its interactive view; Escape closes it. The cooking pot, cutting board, skillet and stove are included.

## Verified behavior

- Both release builds and six focused unit tests passed.
- A real Minecraft client integration test with the original Farmer's Delight Refabricated release opened, rendered and closed all four stations.
- The actual pot recipe-book selection filled the pot; stirring produced cooked rice.
- Library tests exercised camera focus, input, client/server messages, block/item models, transparent geometry and lighting.
- The local player test world launched successfully, and the player reported that it worked.

Automated tests do not establish full compatibility with arbitrary servers or modpacks. Dedicated-server behavior has not been separately playtested.

## Known limitations

- Diegesis uses the normal-scene rendering fallback. Supersampling and the forced foreground overlay are not implemented in this port.
- Optional Sable moving-structure integration is excluded.
- Optional cooking addons, recipe viewers, Sodium/Iris and shader packs have not been playtested. Some optional integrations still compile against upstream 1.21.1 addon APIs; do not assume they are compatible.
- This is a Fabric-only port. Retained upstream files mentioning NeoForge or older Minecraft versions do not represent supported builds.

## Build from source

Install JDK 21. Keep both project directories side by side, as provided here. From the repository root:

```sh
sh diegesis-1.21.11/gradlew -p diegesis-1.21.11 build
sh handson-delight-1.21.11/gradlew -p handson-delight-1.21.11 build
```

Each project's `build/libs/` contains its distributable JAR. The addon compiles against the library's named development JAR, so build Diegesis first. The active root Gradle scripts select the port's dependencies; legacy upstream properties and per-project READMEs are retained for provenance. Both projects use Gradle 9.6.1 and Loom 1.17.19.

For the real-client integration test, build both projects, obtain the official Farmer's Delight dependency above and run:

```sh
sh diegesis-1.21.11/gradlew -p diegesis-1.21.11 runClientGameTest \
  -PportIntegration \
  -PportFdJar=/absolute/path/to/FarmersDelight-1.21.11-3.6.16+refabricated.jar \
  -PportAddonJar=/absolute/path/to/handson-delight-fabric-1.21.11-1.5.0-port.1.jar
```

The client test requires a graphical environment. The test fixture is excluded from production JARs. Run `runClient` instead to open a development client with these same integration dependencies.

## Credits and license

Original mods, assets and APIs by **Pocky**. Original MIT notices are preserved in each project and in the release JARs. See [LICENSE](LICENSE).

Port starting points:

- Diegesis: [`13a02d6854480fe6a8d96a18cbb3237dc4361cd5`](https://github.com/Pocky-l/diegesis/commit/13a02d6854480fe6a8d96a18cbb3237dc4361cd5), 1.0.0-beta.11 for 1.21.1.
- Hands-On Delight: [`081af5da893997f93d295e39d1259dac255b4654`](https://github.com/Pocky-l/handson-delight/commit/081af5da893997f93d295e39d1259dac255b4654), 1.5.0 for 1.21.1.

Unofficial port work prepared for Jwa-koder with Codex assistance. Please report port-specific issues here rather than asking the original author to support this build.
