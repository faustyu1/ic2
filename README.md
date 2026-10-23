# IndustrialCraft 2 — Fabric 1.21.1 port

An unofficial, community-made port of IndustrialCraft 2 (`2.8.222-ex112`, Minecraft 1.12.2 / Forge)
to **Minecraft 1.21.1 / Fabric**.

> This is a fan project. It is not affiliated with, endorsed by, or supported by the IndustrialCraft 2
> team, Mojang Studios, or Microsoft. Please do not report issues with this port to the original authors.

## Disclaimer and ownership

We are a community project and **do not claim any ownership** of IndustrialCraft 2 or its content.

- **IndustrialCraft 2** — its name, design, gameplay, textures, models, sounds and other assets — belongs
  to the IndustrialCraft 2 team and its original authors (Alblaka, Player, RichardG, Thunderdark,
  GregoriusT, alexthesax, Drashian, Elementalist, Feanturi, Lurch1985, SirusKing, tahu44, Aroma1997,
  estebes, Chocohead and other contributors). Official site: <https://www.industrial-craft.net>.
- **Minecraft** is a trademark of Mojang Studios / Microsoft. This project is not an official Minecraft
  product and is not approved by or associated with Mojang or Microsoft.
- **Fabric**, **Fabric API**, **Roughly Enough Items** and **EJML** belong to their respective authors and
  are used under their own licenses.

All rights to the original work remain with their respective owners. The port exists to keep the mod
playable on modern Minecraft versions. If you are a rights holder and have concerns about this
project, please open an issue and we will respond promptly, including taking content down on request.

## Status

Work in progress. Most of the original content (energy net, machines, generators, cables, tools,
armor, crops, UU-matter, worldgen) is ported; expect bugs and missing features.

## Requirements

- Minecraft 1.21.1
- Fabric Loader ≥ 0.16.0
- Fabric API
- Java 21
- Optional: Roughly Enough Items (recipe viewer integration)

## Building

Requires **JDK 21**. The Gradle wrapper is included.

```sh
./gradlew build
```

The jar is written to `build/libs/`.

Run a development client:

```sh
./gradlew runClient
```

## Publishing

Releases are uploaded with the [mod-publish-plugin](https://github.com/modmuss50/mod-publish-plugin).
Set `modrinth_project_id` / `curseforge_project_id` in `gradle.properties`, then:

```sh
MODRINTH_TOKEN=... scripts/publish-modrinth.sh
CURSEFORGE_TOKEN=... scripts/publish-curseforge.sh
```

`--dry-run` builds and checks the upload without sending it. Release notes can be passed in the
`CHANGELOG` environment variable. Tokens are read from the environment only; never commit them.

## License

The code written for this port is shared for the community. The original IndustrialCraft 2 code and
assets are the property of their authors (see above) and are **not** relicensed by this project.
