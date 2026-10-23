# IndustrialCraft 2 Community Edition

**IC2 Community Edition** is an unofficial, community-made port of IndustrialCraft 2
(`2.8.222-ex112`, Minecraft 1.12.2 / Forge) to **Minecraft 1.21.1 / Fabric**.

> "Community Edition" marks this as a fan project, not an official IC2 release. It is not affiliated
> with, endorsed by, or supported by the IndustrialCraft 2 team, Mojang Studios, or Microsoft. Please
> do not report issues with this port to the original authors.

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

The Modrinth project page (summary, description, links, icon) lives in `scripts/modrinth/` and is
pushed with `python3 scripts/modrinth/setup_project.py`.

`--dry-run` builds and checks the upload without sending it. Tokens are read from the environment
only; never commit them.

### Versioning

Versions follow [Semantic Versioning](https://semver.org/):
`MAJOR.MINOR.PATCH[-alpha.N|-beta.N|-rc.N]`, set as `mod_version` in `gradle.properties`.

- The jar and `fabric.mod.json` carry `<version>+mc<Minecraft version>`, e.g.
  `0.1.0-alpha.1+mc1.21.1`; the same version built for another Minecraft version only changes the
  suffix.
- While the version is `0.x`, a MINOR bump may add content or break existing worlds (noted in the
  changelog); a PATCH bump only fixes bugs. `1.0.0` marks full parity with the original and a stable
  world format.
- `-alpha.N` is published as an alpha, `-beta.N` and `-rc.N` as a beta, no suffix as a release.
- Release notes are the version's section in [CHANGELOG.md](CHANGELOG.md); publishing fails without
  one.
- Each published release is tagged `v<version>` (e.g. `v0.1.0-alpha.1`) by the publish scripts.

## License

The code written for this port is released under the
[GNU Lesser General Public License v3.0](LICENSE). The original IndustrialCraft 2 code and assets
remain the property of their authors (see above) and are **not** relicensed by this project.
