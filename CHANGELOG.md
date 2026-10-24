# Changelog

All notable changes to IC2 Community Edition are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow
[Semantic Versioning](https://semver.org/): `MAJOR.MINOR.PATCH[-alpha.N|-beta.N|-rc.N]`, published
as `<version>+mc<Minecraft version>` and tagged `v<version>`.

The section of the version being released becomes the release notes on Modrinth and CurseForge.
Add new entries under a section for the next version as they land.

## [Unreleased]

## [0.1.0-alpha.2]

### Added

- Optional [Jade](https://modrinth.com/mod/jade) integration: the EU in a block's buffer as Jade's
  energy bar and the progress of a working machine
- The metal former's mode button: extruding, rolling and cutting
- The missing sounds: the chainsaw and the switched on nano saber in hand, the saber's swing, the
  jetpack's engine, the drill breaking blocks, the treetap, the insulation cutters, the OD scanner
  and the reactor's Geiger counter speeding up with its output
- Fluid cells scoop up and pour out water, lava and the IC2 fluids that have a block, like buckets
- Liquid construction foam as a block; wading through it slows, and the foam sprayer sucks it up
- The cropnalyzer takes single-use batteries
- The personal safe's door swings open while the safe is in use
- The magnetizer's boots slot

### Changed

- Machine recipes are datapack recipes, one IC2 recipe type per machine (`ic2:macerator`,
  `ic2:thermal_centrifuge`, ...) under `data/ic2/recipe/<machine>/`: datapacks can change them,
  `/reload` picks the changes up and clients of a server receive them. The machine recipe ini
  files are gone from the jar
- Text that does not fit a machine's display is centred on it or drawn smaller, so translations
  no longer run off it; long names in the scanner's results are cut short
- An empty fluid gauge shows only its frame instead of a dark slot
- No radiation for players in creative or spectator mode
- The remaining Russian names are translated

### Fixed

- Tin and lead ore, crushed and purified ores and metal dusts smelt in the vanilla furnace and blast
  furnace, and resin into rubber in the furnace; they only smelted in IC2's own furnaces before,
  so there was no way to tin ingots early on
- Two machine recipes the port could not read: compressing air into an empty fluid cell and
  extracting rubber from a rubber sapling (which also has its UU-matter value again)
- Recipes taking rubber wood (extractor, block cutter) never matched
- Worlds with bare cables crashed on load: a cable not yet in the energy net asked it for its
  current
- Pouring a bucket into a machine (the liquid heat exchanger, for one) gave back two empty buckets
- The jetpack's engine sound kept playing after switching worlds
- The solar helmet did not charge a battery pack on the back under a plain chestplate
- A broken wind or water kinetic generator left its rotor hanging in the air
- The rubber tree's leaves were white
- The steam generator laid the player's inventory over its window
- Short window titles sat off centre next to the upgrade slots

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
- Armor with jetpacks, nano and quantum suits, armor HUD and radiation; jetpacks, battery packs
  and the CF pack also fit the [Trinkets](https://modrinth.com/mod/trinkets) back slot
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

- Requires [Trinkets](https://modrinth.com/mod/trinkets)
- One wrench instead of three; no empty cell, sheets, scaffolds or iron door

- Uses Minecraft's copper instead of IC2's own; raw metal can be crushed
- Metals from other mods come out of IC2 machines through common tags
