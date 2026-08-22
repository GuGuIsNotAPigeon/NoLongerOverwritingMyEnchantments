# AGENTS.md — No Longer Overwriting My Enchantments

## Project overview

Fabric mod (modid: `nlome`, package: `top.g2inp.nlome`) that prevents villager trades for favorited enchantments from being overwritten by workstation destruction or trade cycling. Uses Mixin to inject into `Villager` methods, Fabric Attachment API for per-villager protection state, and custom network payloads for client-server config sync.

## Branch structure

Each Minecraft version range lives on its own branch. **They are NOT subdirectories** — the repo root is the project root on every branch.

| Branch | Minecraft |
|---|---|
| `main` | 1.21.11 |
| `1.21.9-1.21.10` | 1.21.9 |
| `1.21.5-1.21.8` | 1.21.5 |
| `1.21.1-1.21.4` | 1.21.1 |

When making a change that applies to multiple versions, cherry-pick or replicate across branches.

## Build

```sh
./gradlew build
```

Artifact: `build/libs/nlome-1.0.0.jar`. Java 21 target. Uses Fabric Loom with official Mojang mappings.

## Architecture

- **Entrypoints** (declared in `fabric.mod.json`):
  - `main`: `NoLongerOverwritingMyEnchantments` — registers payloads, attaches server-side handlers
  - `client`: `NoLongerOverwritingMyEnchantmentsClient` — receives sync/intercepted payloads, shows toasts
  - `modmenu`: `ModMenuIntegration` — wires the config screen into Mod Menu
- **Mixin** (`VillagerMixin`): injects `@Inject` into `setVillagerData`, `setOffers`, and `updateTrades` on `net.minecraft.world.entity.npc.Villager`
- **Protection logic** (`ProtectionHandler`): uses Fabric's `AttachmentRegistry` to store `ProtectionData` (station, saved offer, break count) on Villager entities. Tracks protected stations in a `Map<GlobalPos, UUID>`
- **Config** (`FavoritesManager`): singleton reading/writing `nlome-favorites.json` in the Fabric config dir. Stores a list of `ResourceKey<Enchantment>` and a `breakThreshold`
- **Network** (`ModPayloads`): three custom payloads — `SetConfigPayload` (C2S), `SyncConfigPayload` (S2C on join), `InterceptedPayload` (S2C toast)
- **Client config** (`ClientFavorites`): static-holder mirror of server config for the config screen

## Key dependencies

- Fabric API, Fabric Loader, Mod Menu (required)
- Trade Cycling (optional, suggested — the `trade-cycling/` directory is gitignored)

## CI

- `build.yml`: runs `./gradlew build` on PRs and pushes. Release workflow (on tags) builds all four branches, renames JARs with MC version, and publishes a GitHub release.
- `qodana_code_quality.yml`: JetBrains Qodana scan on PRs and pushes to `main`.