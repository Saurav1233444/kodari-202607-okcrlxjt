# InstancedDungeons Architecture

## Runtime boundaries

The plugin separates Bukkit/Paper integration from dungeon domain logic. Domain services operate on identifiers and immutable state, while adapters handle worlds, entities, inventories, economy providers, external plugins, and persistence.

The primary runtime flow is:

1. A party is created and validated.
2. The dungeon definition is loaded through a repository.
3. The instance service creates a unique instance record and clones the template world.
4. Players are teleported only after the instance world is loaded and validated.
5. The dungeon engine advances stages and objectives from events and scheduled ticks.
6. Completion or failure distributes rewards, publishes lifecycle events, and schedules instance cleanup.
7. Startup recovery identifies persisted instances without an active runtime and removes or resumes them according to configuration.

## Package responsibilities

```text
com.kodari.instanceddungeons
├── api
│   ├── DungeonPlatform
│   ├── provider
│   └── event
├── commands
├── config
├── database
│   ├── connection
│   ├── migration
│   └── transaction
├── dungeon
│   ├── definition
│   ├── engine
│   └── type
├── editor
├── gui
├── instance
├── listeners
├── loot
├── managers
├── missions
├── party
├── providers
├── repositories
├── rewards
├── scheduler
├── services
├── spawners
├── stages
├── tower
└── utils
```

## Core services

- `PartyService` owns party membership, invitations, leadership, ready checks, chat routing, and offline-player transitions.
- `DungeonDefinitionService` validates and loads dungeon definitions.
- `InstanceService` owns instance creation, world lifecycle, player placement, cleanup, and crash recovery.
- `DungeonRuntimeService` coordinates stages, objectives, timers, gates, spawners, and completion state.
- `RewardService` resolves reward providers and performs economy, item, XP, and key payouts.
- `EditorService` owns editor sessions, selections, previews, and persistence of authored dungeon data.
- `ProtectionService` enforces instance-specific interaction rules without inspecting inventory titles or world names alone.

## Persistence

Repositories expose domain-oriented interfaces. SQLite is the default embedded backend. MySQL, MariaDB, and PostgreSQL use the same repository contracts and connection abstraction. Schema migrations run before repositories are opened. Runtime state is persisted at instance creation, player state changes, stage transitions, and terminal completion.

## World lifecycle

Template worlds are read-only from the instance service's perspective. Every instance receives a generated identifier and isolated world folder. World creation, loading, unloading, and deletion are serialized through the instance lifecycle scheduler. Player teleports occur on the main thread after asynchronous filesystem work completes. Cleanup refuses to delete a world containing players and retries after evacuation.

## Extension points

Dungeon types, objectives, spawner providers, loot item providers, reward providers, placeholders, and external integrations are registered through explicit interfaces. External integrations are optional and are detected at startup, allowing the core plugin to operate without Vault, PlaceholderAPI, WorldEdit, MythicMobs, ItemsAdder, Oraxen, Nexo, or ExecutableItems.

## GUI framework

The reusable GUI layer uses inventory-scoped handlers and button actions. Each open inventory is registered by identity, click handling is cancelled before dispatch, and closed inventories are unregistered. Paginated editors and reward interfaces build on the same base components.

## Threading rules

Paper API calls that touch worlds, entities, inventories, or players run on the server thread. Filesystem cloning, database queries, migrations, loot-table loading, and cleanup discovery run asynchronously. Services return completion stages where an operation crosses thread boundaries, and lifecycle transitions are guarded against duplicate completion.

## Initial implementation sequence

The project is built in dependency order:

1. Configuration and plugin lifecycle.
2. Domain identifiers, immutable models, repositories, and database migrations.
3. Party service and party events.
4. Instance lifecycle and world cloning.
5. Dungeon definitions, stages, objectives, and runtime engine.
6. Protection, timer, spawner, gate, loot, and reward services.
7. Commands, editor sessions, GUIs, and external integrations.
8. Recovery, metrics, API documentation, and tests.