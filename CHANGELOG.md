# Changelog

All notable changes to IC2 Community Edition are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow
[Semantic Versioning](https://semver.org/): `MAJOR.MINOR.PATCH[-alpha.N|-beta.N|-rc.N]`, published
as `<version>+mc<Minecraft version>` and tagged `v<version>`.

The section of the version being released becomes the release notes on Modrinth and CurseForge.
Add new entries under a section for the next version as they land.

## [0.1.0-alpha.1]

First public alpha: a port of IndustrialCraft 2 `2.8.222-ex112` (Minecraft 1.12.2 / Forge) to
Minecraft 1.21.1 / Fabric. Expect bugs and missing features, and back up your worlds.

### Added

- EU energy net with cables, signal cables, transformers and storage blocks
- Processing machines: macerator, extractor, compressor, iron, electric and induction furnaces,
  ore washing plant, metal former, block cutter, recycler, blast furnace, solid canner and others
- Generators: fuel, geothermal, solar, wind, water, kinetic generators, steam boiler and turbine
- Fluid machines, tanks and pipes with pump covers
- UU-matter chain: matter and mass fabricators, scanner, pattern storage, replicator
- Miner, advanced miner and mining pipe
- Electric tools, mining laser, wrenches and crowbar
- Armor with jetpacks, nano and quantum suits, armor HUD and radiation
- Crops: crop sticks, cross-breeding, Crop-Matron and crop harvester
- Logistics and personal blocks: item buffers, sorting machine, weighted item distributor, batch
  crafter, industrial workbench, trade-o-mat, energy-o-mat
- Teleporter, terraformer with blueprints, chunk loader, IC2 boats
- Explosives: dynamite, industrial TNT, nuke
- Construction foam, painter, cable foam and the obscurator
- Rubber trees and ores in world generation
- Optional Roughly Enough Items integration with recipe transfer
- `config/IC2.ini` support

### Changed

- Uses Minecraft's copper instead of IC2's own; raw metal can be crushed
- Metals from other mods come out of IC2 machines through common tags
