# ⚔ WagerPlugin

A Minecraft Spigot plugin for PvP wager matches with arenas, GUIs, and economy integration.

## Requirements
- **Spigot/Paper 1.20+** (Java 17)
- **Vault** + an economy plugin (EssentialsX, CMI, etc.)
- **WorldEdit** (optional, for auto-pasting schematics)

## Features
- `/wager` — Opens clean GUI showing all active wagers, create new ones
- Click to accept wagers with confirmation menu
- Preset amounts ($100 - $100K) + custom amounts (supports `1k`, `5.5k`, `1m`, `1,000` formats)
- Multiple simultaneous arenas — no queue waiting
- Auto-loads schematics from `plugins/WagerPlugin/schematics/`
- 3% tax on winnings (configurable)
- Win/loss title screen with money amounts
- "WAGER STARTING" title + countdown
- `/leave` to return to lobby (forfeits if in fight)
- Inventory save/restore, death handling, disconnect = forfeit

## Setup

### 1. Install
Drop `WagerPlugin.jar` into your `plugins/` folder along with Vault and an economy plugin.

### 2. Set Lobby
Go to where you want players to return after wagers:
```
/arena setlobby
```

### 3. Create Arenas

**Option A: Manual**
```
/arena create myarena
/arena world              # teleport to arena world
# build your arena
/arena setspawn 1 myarena # stand at player 1 spawn
/arena setspawn 2 myarena # stand at player 2 spawn
```

**Option B: Schematics (requires WorldEdit)**
1. Drop `.schem` files into `plugins/WagerPlugin/schematics/`
2. Run `/arena reload` — arenas auto-create and paste
3. Set spawns: `/arena setspawn 1 <name>` and `/arena setspawn 2 <name>`

### 4. Done!
Players use `/wager` to open the menu.

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/wager` | `wager.use` | Open wager GUI |
| `/leave` | — | Leave wager / return to lobby |
| `/arena create <name>` | `wager.admin` | Create arena |
| `/arena delete <name>` | `wager.admin` | Delete arena |
| `/arena setspawn <1\|2> <arena>` | `wager.admin` | Set spawn point |
| `/arena setlobby` | `wager.admin` | Set lobby return point |
| `/arena list` | `wager.admin` | List all arenas + status |
| `/arena world` | `wager.admin` | Teleport to arena world |
| `/arena reload` | `wager.admin` | Reload config + schematics |

## Config (`config.yml`)
```yaml
tax-percent: 3.0          # Tax on winnings
min-wager: 100             # Minimum wager amount
max-wager: 1000000         # Maximum wager amount
countdown-seconds: 5       # Countdown before fight
arena-world: "wager_arenas" # Flat world for arenas

preset-amounts:            # Buttons shown in create menu
  - 100
  - 500
  - 1000
  - 5000
  - 10000
  - 50000
  - 100000
```

## How Wagers Work
1. Player creates wager via `/wager` → appears in listing
2. Another player clicks the listing → confirmation screen
3. Both players teleport to an available arena
4. 5-second countdown with titles
5. Fight! Death = loss
6. Winner gets pot minus 3% tax, both return to lobby
7. Arena is freed for next match

## Building from Source
```bash
mvn clean package
# Output: target/WagerPlugin.jar
```
