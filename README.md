# Don't Claim Our Structures!

A server-side Forge mod for Minecraft 1.20.1 that prevents players from claiming chunks containing structures. Protects villages, temples, monuments, and other important structures from being claimed and griefed.

Support on [Discord](https://discord.gg/m4EHeRjfZ9)

## Supported Claiming Mods

- **FTB Chunks** - Blocks claims before they are registered (no flicker)
- **Open Parties and Claims (OPaC)** - Mixin-based interception at source, with a fallback listener mode

## Features

- Prevents claiming chunks that contain protected structures
- Supports both surface and underground structure protection
- Structure whitelist/blacklist filtering
- Dimension and biome filtering
- Configurable padding zones around structures
- OP bypass with configurable permission level
- Optional singleplayer protection (disabled by default)
- LRU cache with TTL for performance
- Custom denial messages with placeholders
- In-game admin commands for debugging and management

## Commands

All commands require OP level 2.

| Command | Description |
|---------|-------------|
| `/dtos reload` | Reload config and clear cache |
| `/dtos check [x z]` | Check protection status of a chunk |
| `/dtos debug [x z]` | List all structures in a chunk |
| `/dtos cache clear` | Clear the protection cache |
| `/dtos cache stats` | Show cache statistics |

## Configuration

Config file: `dont_touch_our_structures-common.toml`

### General
- `affect_singleplayer` - Enable protection in singleplayer (default: `false`)
- `bypass_with_op` - Allow OPs to bypass protection (default: `true`)
- `op_bypass_level` - Minimum OP level for bypass (default: `2`)

### Structure Types
- `protect_surface` - Protect surface structures (default: `true`)
- `protect_underground` - Protect underground structures (default: `false`)

### Filtering
Structure, dimension, and biome filtering each support three modes:
- `WHITELIST` - Only listed entries are affected
- `BLACKLIST` - All entries except listed ones are affected
- `DISABLED` - No filtering

### Padding
- `enable` - Add a buffer zone around structures (default: `false`)
- `default_chunks` - Default padding radius in chunks (default: `1`)
- `custom_structure_padding` - Per-structure padding overrides (e.g. `"minecraft:monument=3"`)

### Messages
- `custom_denial_message` - Custom message with `{structure_name}`, `{structure_type}`, `{chunk_x}`, `{chunk_z}` placeholders
- `include_structure_name` / `include_structure_type` / `include_coordinates` - Toggle auto-generated message details

## Hosting

If you'd like to support me and the development of my mods, I recommend trying out BisectHosting. Use code "project8gbderam" to get 25% off your first month of a gaming server for new customers.

[![Bisect](https://www.bisecthosting.com/partners/custom-banners/54bb107c-f9fc-4f32-8515-fb4e3d56c124.png)](https://www.bisecthosting.com/project8gbderam)

## License

MIT
