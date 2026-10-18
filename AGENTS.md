# AGENTS.md — No Longer Overwriting My Enchantments

## Project overview

Fabric mod (modid: `nlome`, package: `top.g2inp.nlome`) that prevents villager trades for favorited enchantments from being overwritten by workstation destruction or trade cycling. Uses Mixin to inject into `Villager` methods and the Fabric Attachment API for per-villager protection state.

**Single-player only.** The mod declares `environment: "client"` and has no networking of any kind. See "Single-player design" below before adding server support.

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

Artifact: `build/libs/nlome-1.0.2.jar`. Java 21 target. Uses Fabric Loom with official Mojang mappings.

## Architecture

- **Entrypoints** (declared in `fabric.mod.json`):
  - `main`: `NoLongerOverwritingMyEnchantments` — registers the block-break hook, calls `ProtectionHandler.register()`
  - `client`: `NoLongerOverwritingMyEnchantmentsClient` — registers the `key.nlome.open_config` keybind (default K) that opens `ConfigScreen`
  - `modmenu`: `ModMenuIntegration` — wires the config screen into Mod Menu (only loaded when Mod Menu is installed)
- **Mixin** (`VillagerMixin`): injects `@Inject` into `setVillagerData`, `setOffers`, and `updateTrades` on `net.minecraft.world.entity.npc.Villager`
- **Protection logic** (`ProtectionHandler`): uses Fabric's `AttachmentRegistry` to store `ProtectionData` (station, saved offer, break count) on Villager entities. Tracks protected stations in a `Map<GlobalPos, UUID>`. Locking is decided **per villager** (`findFavoritedOffer != null && getVillagerXp() > 0`), not by attachment or station lookup, and entering that state stamps the attachment when missing so `VillagerMixin` still blocks the profession reset. Each villager keeps at most one station binding — re-binding and unbinding always clear that villager's stale coordinates. Exposes `ToastHelper.showIntercepted` directly from server-side code
- **Config** (`FavoritesManager`): singleton reading/writing `nlome-favorites.json` in the Fabric config dir. Stores a list of `ResourceKey<Enchantment>` and a `breakThreshold`. For trade-refresh interception the threshold is the number of blocked refreshes (`breaks > breakThreshold` allows the next one); the workstation-break path reuses the same value but does not count the adopting break
- **Config screen** (`ConfigScreen`): opened from Mod Menu or the `K` keybind; lists every enchantment, searches by translated name or ID, sorts A-Z, edits the break threshold

## Single-player design

The mod is intentionally client-environment and network-free; keep it that way unless server support is an explicit goal.

- The config screen writes the same `<config>/nlome-favorites.json` the protection logic reads, which works because the integrated server shares the client's JVM and config directory. There is no sync packet to keep a dedicated server in step.
- `ToastHelper` is annotated `@Environment(EnvType.CLIENT)` yet called from `ProtectionHandler`, which runs on the (integrated) server thread. This only works because the client classes are present in the same JVM.
- Protection state lives in a non-persistent attachment (`AttachmentRegistry.create` without `.persistent(...)`), so break counts and station bindings reset when the entity is reloaded. Protection itself recovers by rescanning current offers.

## Key dependencies

- Fabric API, Fabric Loader (required)
- Mod Menu (optional, only for the config screen)
- Trade Cycling (optional, suggested — the `trade-cycling/` directory is gitignored). For dev runs, keep the jar in `trade-cycling/` and copy it to `run/mods/` (also gitignored); Trade Cycling refreshes trades by calling `Villager.setOffers(null)`, which is exactly the hook `VillagerMixin` intercepts

## CI

- `build.yml`: runs `./gradlew build` on PRs and pushes. Release workflow (on tags) builds all four branches, renames JARs with MC version, and publishes a GitHub release.
- `qodana_code_quality.yml`: JetBrains Qodana scan on PRs and pushes to `main`.