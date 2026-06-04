# Pipes v0.2.0 — Roadmap

Reimplementing good ideas from the reviewed `dev` branch as clean, incremental improvements on `main`.

## Phases

- [x] **Phase 1: PDC migration** — Switch entity tags from scoreboard tags to PersistentDataContainer. Hybrid read (PDC first, scoreboard fallback) for backwards compat. PDC-only writes. Auto-migrate on chunk load.
- [x] **Phase 2: Per-world PipeManager** — One PipeManager per world via `WeakHashMap<World, PipeManager>`. New `WorldManager` for world load/unload lifecycle. Switch to Paper's `world.submitCyclicalTask()`. Config to enable/disable pipes per world.
- [ ] **Phase 3: Container adapters** — Extract inventory logic into `ContainerAdapter` interface. Implementations for vanilla containers, furnaces (vanilla hopper parity), and brewing stands (no extract during brewing). `ContainerAdapterRegistry` for lookup.
- [ ] **Phase 4: Path caching + sleep/throttle** — Cache computed paths with reverse-index invalidation. Validate all chain members (not just destination). Sleep idle pipes (empty source / full dest). Use ticks consistently. Transfer phase offset to spread load.
- [ ] **Phase 5: Corner pipe improvements** — Multi-output display entities, DOWN-facing head displays, junction fallback routing (`tryCornerJunctionAlternatives`, `tryAlternativeDestination`), UP direction support.
- [ ] **Phase 6: Oxidation system** — Config-gated copper aging. Waxing (honeycomb) and scraping (axe). Batch `tickOxidation()` with single cache eviction pass. Disableable via `oxidation.enabled: false`.
- [ ] **Phase 7: Listener package reorg** — Move all listeners into `listener/` subpackage.

---

## Phase 1: PDC Migration

**Goal:** Switch from scoreboard tags to PersistentDataContainer for entity identification. Backwards compatible.

**Strategy — hybrid read, PDC-only write:**
- `getPipeTag(Entity)` — check PDC first, fall back to scoreboard tag scan
- `addPipeTag(Entity, String)` — write to PDC, remove any old scoreboard tag
- `isPipeEntity(Entity)` — check PDC first, fall back to scoreboard tag scan
- On chunk scan (`scanChunk`), auto-migrate: if entity has scoreboard tag but no PDC, write PDC + remove scoreboard tag
- Add `_head` suffix support for corner head displays (needed later in Phase 5)
- Log migration count at INFO level

**Files to modify:**
- `PipeTags.java` — rewrite API from `Set<String>` to `Entity`-based, add PDC key, keep scoreboard fallback
- `PipeManager.java` — update all callers (spawn, scan, cleanup, remove) to use new `Entity`-based API

**Why PDC over scoreboard tags:**
- O(1) namespaced key lookup vs O(n) string iteration
- No collision risk with other plugins
- Modern Bukkit best practice

---

## Phase 2: Per-world PipeManager

**Goal:** Isolate pipe state per world for multi-world servers.

**Changes:**
- `PipesPlugin`: replace `private PipeManager pipeManager` with `WeakHashMap<World, PipeManager>`
- New `WorldManager.java`: listens for `WorldLoadEvent`/`WorldUnloadEvent`, creates/shuts down per-world PipeManagers
- `PipeManager` constructor takes `(PipesPlugin, World)`, stores world reference
- Switch from `Bukkit.getScheduler().runTaskTimer()` to Paper's `world.submitCyclicalTask()`
- `PipeListener`, commands, and `notifyBlockChanged()` route to correct manager via `pipeManager.get(location.getWorld())`
- Chunk scan scoped to the manager's world
- Randomize transfer task offset per-manager to spread load across worlds
- Config to enable/disable pipes per world (allowlist or blocklist mode)

### Per-world config (`config.yml`)
```yaml
worlds:
  mode: allowlist  # "allowlist" or "blocklist"
  list:
    - world
    - world_the_end
```
- `allowlist` mode: pipes only work in listed worlds
- `blocklist` mode: pipes work everywhere except listed worlds
- Default: no filtering (all worlds enabled)
- `WorldManager` checks config before creating a PipeManager for a world
- Pipe placement blocked in disabled worlds (cancel `BlockPlaceEvent` with message)

**Files to modify:**
- `PipesPlugin.java` — WeakHashMap, routing helpers
- `PipeManager.java` — constructor, world-bound tasks, remove world params from methods
- `PipeListener.java` — route events to correct manager, block placement in disabled worlds
- `PipeConfig.java` — parse world filter config
- New `WorldManager.java`

---

## Phase 3: Container Adapters

**Goal:** Decouple inventory logic from PipeManager. Enable proper furnace/brewing stand handling.

**New interface — `adapter/ContainerAdapter.java`:**
- `canReceive(Block)` — can this container accept items?
- `insert(Block, ItemStack)` — insert items, return leftover
- `peekExtract(Block, int maxAmount)` — preview extraction without modifying inventory
- `commitExtract(Block, ItemStack)` — actually remove items after successful transfer
- `peekExtractMatching(Block, int maxAmount, ItemStack template)` — extract only matching items
- `hasItems(Block)` — any items present?
- `requestedItem(Block)` — what item does this container want? (e.g., furnace fuel slot)

**Implementations:**
- `VanillaContainerAdapter` — wraps `Container.getInventory().addItem()` for chests, hoppers, etc.
- `FurnaceContainerAdapter` — fuel slot only if matching stack exists (vanilla hopper parity), smeltable check for input
- `BrewingStandContainerAdapter` — no extraction during active brewing, bottle slot aggregation

**Registry — `ContainerAdapterRegistry.java`:**
- `findAdapter(Block) -> Optional<ContainerAdapter>`
- Priority: BrewingStand > Furnace > vanilla Container

**PipeManager refactor:**
- `transferItems()`: replace `sourceBlock.getState() instanceof Container` with adapter lookup
- `findDestination()`: replace `Container` check with `adapter.canReceive()`
- `categorizeSourceBlock()`/`categorizeDestinationBlock()`: use adapter registry for "container" category

**Files:**
- New `adapter/` package (4 files)
- New `ContainerAdapterRegistry.java`
- `PipeManager.java` — refactor transfer and pathfinding

---

## Phase 4: Path Caching + Sleep/Throttle

**Goal:** Major performance win for large pipe networks.

### Path caching
- `CachedPath` record: `(destination, lastPipeLocation, pipeChain, minItemsPerTransfer)`
- `pathCache: Map<Location, CachedPath>` — keyed by pipe start location
- `chainMembership: Map<Location, Set<Location>>` — reverse index (pipe member → set of path starts that include it)
- `dirtyPaths: Set<Location>` — marked dirty on register/unregister
- `getOrBuildPath()` — check cache → validate → rebuild if stale
- `evictCacheEntry(key)` — remove single entry + clean reverse index
- `evictCacheByMember(location)` — evict all paths passing through a location (O(fanout), not O(total))
- `isPathStillValid()` — **must validate ALL chain members**, not just destination (fix from review)

### Sleep/throttle
- `sleepUntil: Map<Location, Long>` — **use ticks, not millis** (fix dev branch bug)
- `sleepPipe(location, ticks)` — sets wake tick
- Source empty → sleep for `source-empty-ticks` (config, default 60)
- Dest full → sleep for `dest-full-ticks` (config, default 80)
- `wakeUpPipe()` — called from `notifyBlockChanged()` when adjacent block changes
- `nullDestRecheckUntil` — recheck interval for dead-end paths (config, default 40 ticks)

### Transfer phase offset
- Hash pipe location to spread transfers across ticks
- `isTransferPhase(currentTick, location, intervalTicks)` — avoids all pipes firing on same tick

### Config additions (`config.yml`)
```yaml
performance:
  sleep:
    source-empty-ticks: 60
    dest-full-ticks: 80
    end-recheck-ticks: 40
```

**Files:**
- `PipeManager.java` — caching, sleep, phase offset
- `PipeConfig.java` — new config fields
- `config.yml` — new section

---

## Phase 5: Corner Pipe Improvements

**Goal:** Better visuals and smarter routing for corner/junction pipes.

### Display improvements
- `refreshCornerDisplayEntities()` — reconcile desired vs actual display entities (create missing, remove stale)
- `getCornerActiveOutputFaces()` — primary facing + adjacent containers/compatible pipes
- Separate head display entity for DOWN-facing corners (`_head` tag suffix)
- Per-direction vertical/forward offsets in `display.yml` (`side`, `up`, `down`)
- UP direction textures for corner pipe types

### Routing improvements
- `tryCornerJunctionAlternatives()` — scan path chain for corner junctions, try secondary outputs before dropping
- `tryAlternativeDestination()` — when primary dest is full, try adjacent containers at chain end
- `canRouteIntoAdjacentPipe(face, pipeData, allowCorner)` — controls corner-to-corner connections
- Depth limit: `MAX_FALLBACK_DEPTH = 24`

**Files:**
- `PipeManager.java` — routing logic, display refresh
- `display.yml` — corner-specific offsets
- `DisplayConfig.java` — parse new corner config fields

---

## Phase 6: Oxidation System

**Goal:** Config-gated copper pipe aging that mirrors vanilla copper mechanics. Must be fully disableable.

### Mechanics
- Periodic check: iterate all loaded oxidizable pipes, roll probability per pipe
- Right-click with honeycomb → wax pipe (prevents oxidation)
- Right-click with axe → scrape (remove wax or reverse one oxidation stage)
- `convertPipeVariant()` on PipeManager — updates PipeData, skull texture, display entity textures + PDC tags
- `tickOxidation()` on PipeManager — batch conversions, single cache eviction pass

### Config (`config.yml`)
```yaml
oxidation:
  enabled: true
  check-interval-ticks: 1200
  chance-numerator: 1
  chance-denominator: 16
  transitions:
    copper_pipe: oxidized_copper_pipe
    copper_corner_pipe: oxidized_copper_corner_pipe
  wax-transitions:
    copper_pipe: waxed_copper_pipe
    copper_corner_pipe: waxed_copper_corner_pipe
```

### Performance
- Only iterates pipes once per check interval (~1 min default)
- 1/16 chance per pipe per check — negligible cost even with 1000+ pipes
- Entirely disabled via `oxidation.enabled: false`

**Files:**
- New `listener/OxidationListener.java`
- `PipeManager.java` — `tickOxidation()`, `convertPipeVariant()`
- `config.yml`, `display.yml` — new variants and textures

---

## Phase 7: Listener Package Reorg

**Goal:** Cleaner project structure as listener count grows.

**Moves:**
- `PipeListener.java` → `listener/PipeListener.java`
- `CauldronConversionListener.java` → `listener/CauldronConversionListener.java`
- `RecipeUnlockListener.java` → `listener/RecipeUnlockListener.java`
- `ConversionRecipeCraftListener.java` → `listener/ConversionRecipeCraftListener.java`

Update package declarations and imports in moved files and all referencing files.

Can be done at any point — no functional dependencies.
