
# Copper Pipes

A Minecraft Paper plugin that adds copper pipes for transferring items between containers.

## Overview

Copper Pipes creates an item transport system using custom player head blocks with ItemDisplay entities for visual representation. Pipes can be chained together to move items from one container to another automatically.

## File Structure

```
copper_pipes/
├── src/main/java/com/example/copperpipes/
│   ├── CopperPipesPlugin.java  - Main plugin class, recipe registration, item creation
│   ├── PipeManager.java        - Core pipe logic, item transfer, display entities
│   ├── PipeListener.java       - Event handlers for placement/breaking/chunks
│   ├── PipeTags.java           - Scoreboard tag encoding for persistence
│   └── PipeType.java           - Enum defining REGULAR and CORNER pipe types
├── src/main/resources/
│   ├── plugin.yml              - Plugin metadata
│   ├── config.yml              - User configuration options
│   └── display.yml             - Internal display/texture settings (not user-editable)
└── docs/assets/                - Texture images
```

### Class Responsibilities

**CopperPipesPlugin.java**
- Plugin initialization and shutdown
- Crafting recipe registration for both pipe types
- Item creation with custom textures
- `/copperpipes reload` command handling
- Configuration loading

**PipeManager.java**
- Tracks all active pipes via `Map<Location, PipeData>`
- Handles item transfer between containers
- Spawns and manages ItemDisplay entities
- Calculates 3D transformations for pipe visuals
- Scans for existing pipes on server start
- Runs scheduled transfer and debug particle tasks

**PipeListener.java**
- `BlockPlaceEvent` - Creates pipe with correct orientation and display entity
- `BlockBreakEvent` - Removes pipe and cleans up display entity
- `ChunkLoadEvent` - Restores pipes when chunks load
- `ChunkUnloadEvent` - Unloads pipes from memory

**PipeTags.java**
- Encodes pipe data in scoreboard tags: `copper_pipe:{x}_{y}_{z}_{facing}_{type}`
- Enables pipe persistence across server restarts

**PipeType.java**
- Defines `REGULAR` and `CORNER` pipe types
- Each type has different placement and transfer behavior

## How It Works

### Pipe Types

**Regular Pipes**
- Pull items from the source container behind them
- Face **away** from the block they were placed against

**Corner Pipes**
- Never pull items - only relay items pushed into them
- Face **toward** the block they were placed against
- Cannot be placed on ceilings (DOWN-facing is blocked)

### Crafting Recipes

**Regular Pipe:**
```
[Copper Ingot] [Copper Ingot] [Copper Ingot]
[    Empty   ] [    Empty   ] [    Empty   ]
[Copper Ingot] [Copper Ingot] [Copper Ingot]
```
Produces 1 Copper Pipe.

**Corner Pipe:**
```
[Copper Ingot] [Copper Ingot] [Copper Ingot]
[    Empty   ] [    Empty   ] [Copper Ingot]
[Copper Ingot] [Copper Ingot] [Copper Ingot]
```
Or mirrored horizontally. Produces 1 Corner Pipe.

### Placement

1. Place the pipe item on a block
2. Regular pipes face **away** from the clicked block; corner pipes face **toward** it
3. A player head block is created with the appropriate texture
4. An ItemDisplay entity spawns for the extended visual

### Item Transfer

Pipes run on a scheduled task (default: every 10 ticks / 0.5 seconds):

1. **Regular pipes only:** Check the block opposite the pipe's facing direction (the source)
2. If it's a container with items, extract items (default: 1 per transfer)
3. Follow the pipe's facing direction to find a destination:
   - If another pipe, push items into that pipe
   - If a container, deposit items there
   - If no valid destination, drop items on the ground

**Corner pipes** do not pull items - they only relay items that are pushed into them by other pipes.

**Supported containers:** Any block implementing the Container interface, including chests, trapped chests, hoppers, droppers, dispensers, barrels, shulker boxes, and modded containers.

### Display Entities

Each pipe has an ItemDisplay entity that:
- Scales along the facing direction based on adjacent blocks
- Rotates to match the pipe's orientation
- Adjusts position based on neighboring containers and pipes

## Configuration

Located in `config.yml`:

```yaml
pipe:
  # Display name (supports MiniMessage format)
  name: "<gold>Copper Pipe"

  transfer:
    # Ticks between transfers (20 ticks = 1 second)
    interval-ticks: 10
    # Items moved per operation
    items-per-transfer: 1

corner-pipe:
  name: "<gold>Corner Pipe"

debug:
  # Show debug particles on pipes
  particles: false
  # Particle spawn interval in ticks
  particle-interval: 10
```

Display settings (textures, scaling, offsets) are stored internally in `display.yml` and are not intended for user modification.

## Commands & Permissions

### Commands

| Command | Description |
|---------|-------------|
| `/copperpipes reload` | Reloads configuration and restarts tasks |

### Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `copperpipes.admin` | Access to all commands | op |
| `copperpipes.reload` | Reload configuration | op |

## Persistence

Pipes persist across server restarts via:
1. The player head block remains in the world
2. The ItemDisplay entity has a scoreboard tag encoding location, facing, and type
3. On chunk load, the plugin scans for these entities and re-registers pipes

# TODO:

- fix velocity offset from downward pipes
- "valve" pipe to enable/disable flow
- figure out behavior for pistons pushing pipes?

## maybe

- dispenser pipes
- warp pipes
  - green mario texture, crafted with ender pearls? teleports entities
- dyed pipes?
- filter pipes?
- glass window pipes
  - show items inside the pipe (would add a delay?)
