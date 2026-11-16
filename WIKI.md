# 🪙 UltraSTS (Sell-To-Server) — Comprehensive Architectural Manual & Administrator Wiki

---

## 📑 Table of Contents
1. [Executive Overview](#-executive-overview)
2. [Core Architecture & Valuation Pipeline](#-core-architecture--valuation-pipeline)
   - [Dynamic Valuation Formula Matrix](#dynamic-valuation-formula-matrix)
   - [Accidental Sale Protection & Buyback Subsystem](#accidental-sale-protection--buyback-subsystem)
   - [Multi-Database Architecture (JSON, SQLite, MySQL, MariaDB, Mongo)](#multi-database-architecture-json-sqlite-mysql-mariadb-mongo)
3. [Safety Auditing, Tradeability & Blacklists](#-safety-auditing-tradeability--blacklists)
4. [Command & Subcommand Reference](#-command--subcommand-reference)
5. [Permissions & Privilege Hierarchy](#-permissions--privilege-hierarchy)
6. [Interactive User Interface Guide](#-interactive-user-interface-guide)
7. [Configuration Reference (`config.json` & `sts.json`)](#-configuration-reference-configjson--stsjson)
8. [Enterprise Production Templates](#-enterprise-production-templates)

---

## 📌 Executive Overview

**UltraSTS** (Sell-To-Server) is an enterprise-grade automated Pokémon appraisal and exchange economy mod developed specifically for **Cobblemon** server networks. Designed to provide a healthy, sustainable monetary sink and catch incentive, UltraSTS allows trainers to liquidate excess wild Pokémon directly into server currency based on a sophisticated valuation formula that analyzes individual stat distributions (IVs and EVs), levels, shiny variants, and legendary rarity tiers.

Equipped with a built-in insurance **Buyback Subsystem** to safeguard players against accidental sales, comprehensive leaderboards tracking server trading volume, and multi-database support (JSON, SQLite, MySQL, MariaDB, MongoDB), UltraSTS serves as the primary currency distribution baseline for competitive Cobblemon communities.

### Key Capabilities
- **Algorithmic Dynamic Valuation**: Automatically calculates payout values based on species base price, level, percentage of perfect IVs, EV training investment, and special rarity multipliers.
- **Accidental Sale Insurance (`BuybackMenu`)**: Players who accidentally sell a cherished team member can repurchase it within a configurable grace window (with an optional administrative surcharge fee).
- **Universal Multi-Database Persistence**: Out-of-the-box support for JSON files, SQLite, MariaDB/MySQL via HikariCP, and MongoDB clusters.
- **Untradeable & Starter Safeguards**: Natively verifies Cobblemon's `tradeable=false` flag and custom blacklist tags to prevent players from liquidating starter Pokémon or event rewards.
- **Top Liquidation Leaderboards**: Highlights the server's top Pokémon merchants based on total currency earned and creatures traded.

---

## 🏗️ Core Architecture & Valuation Pipeline

```
┌────────────────────────────────────────────────────────────────────────┐
│                         UltraSTS Valuation Bus                         │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
           ┌─────────────────────────┼─────────────────────────┐
           ▼                         ▼                         ▼
┌────────────────────┐    ┌────────────────────┐    ┌────────────────────┐
│ Valuation Engine   │    │  Buyback Subsystem │    │ Storage Connector  │
│ IV/EV/Shiny Matrix │    │ Grace Window Cache │    │ SQL / Mongo / JSON │
└──────────┬─────────┘    └──────────┬─────────┘    └──────────┬─────────┘
           │                         │                         │
           └─────────────────────────┼─────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Transaction & Safety Bus                        │
│   • Tradeability Check   • Battle Lockout   • Atomic Balance Deposit   │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
           ┌─────────────────────────┴─────────────────────────┐
           ▼                                                   ▼
┌────────────────────────────────────┐    ┌──────────────────────────────┐
│       Relational SQL Backends      │    │     Enterprise MongoDB       │
│  SQLite • MySQL • MariaDB • H2     │    │   High-Throughput Clusters   │
└────────────────────────────────────┘    └──────────────────────────────┘
```

### Dynamic Valuation Formula Matrix
When a player inspects a Pokémon in the sell interface (`STSSellMenu`), the valuation engine computes its payout using the following formula:

$$\text{Final Value} = \left(\text{Base Species Price} + (\text{Level} \times \text{Level Multiplier})\right) \times \left(1 + \frac{\text{IV Total}}{186} \times \text{IV Multiplier}\right)$$
$$\text{If Shiny: } \text{Final Value} \times \text{Shiny Multiplier}$$
$$\text{If Legendary / Mythical: } \text{Final Value} \times \text{Legendary Multiplier}$$

Administrators can override base prices for specific high-demand species inside `sts.json`.

### Accidental Sale Protection & Buyback Subsystem
Accidental sales can lead to player frustration. UltraSTS includes a built-in insurance buffer:
1. When a Pokémon is sold to the server, it is temporarily cached in `BuybackEntry` under the player's account.
2. The player has a configurable duration (e.g., 2 hours) to open `/sts buyback`.
3. The player can repurchase their exact Pokémon—preserving all EVs, IVs, nickname, and ball metadata—for the sale price plus an optional surcharge (e.g., 10% fee).
4. After the buyback window expires, the entry is permanently purged from the database.

### Multi-Database Architecture
Managed via `DatabaseFactory`:
- **JSON**: Simple flat file storage under `config/ultrasts/data/`.
- **SQL (SQLite, MySQL, MariaDB, H2)**: Highly structured schema with indexed transaction tables.
- **MongoDB**: Ideal for multi-server proxy networks where players can access their buyback box from any connected game node.

---

## 🔒 Safety Auditing, Tradeability & Blacklists

- **Battle Safety**: The sell confirmation automatically aborts if the target Pokémon is participating in an active Cobblemon battle.
- **Tradeable Flag Check**: Enforces Cobblemon's `pokemon.getTradeable()`. Untradeable Pokémon cannot be sold.
- **Custom Blacklist**: Species, egg groups, or labels defined in `sts.json` are rejected with a descriptive explanation.

---

## ⌨️ Command & Subcommand Reference

### User Commands (`/sts`)
| Command | Arguments | Permission Node | Description |
| :--- | :--- | :--- | :--- |
| `/sts` | *None* | `ultrasts.user` | Opens the main Pokémon appraisal and sale GUI. |
| `/sts buyback` | *None* | `ultrasts.user` | Opens the buyback menu to reclaim recently sold Pokémon. |
| `/sts top` | *None* | `ultrasts.user` | Displays the server liquidation leaderboard. |
| `/sts profile` | *None* | `ultrasts.user` | Inspects personal statistics (total Pokémon sold, earnings). |

### Administrator Commands (`/sts admin`)
| Command | Arguments | Permission Node | Description |
| :--- | :--- | :--- | :--- |
| `/sts reload` | *None* | `ultrasts.admin` | Hot-reloads all valuation curves, pricing files, and configs. |
| `/sts reset` | `<player>` | `ultrasts.admin` | Resets a player's transaction history and buyback records. |

---

## 🛡️ Permissions & Privilege Hierarchy

```
ultrasts.user                    # Allows opening the STS GUI and selling Pokémon
├── ultrasts.buyback             # Allows accessing the buyback restoration menu
└── ultrasts.top                 # Allows viewing top seller leaderboards

ultrasts.admin                   # Administrative reload and reset commands
```

---

## 🖥️ Interactive User Interface Guide

- **`STSMenu` / `STSSellMenu`**: Displays party and PC Pokémon with real-time value badges, IV breakdown, and confirm buttons.
- **`BuybackMenu`**: Lists all Pokémon currently in the player's insurance grace window with buyback costs and remaining expiration timers.
- **`LeaderBoardMenu`**: Displays top player skulls and cumulative payouts.

---

## ⚙️ Configuration Reference (`config.json`)

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `debug` | `Boolean` | `false` | Enables valuation formula debug logging. |
| `lang` | `String` | `"en"` | Language code identifier. |
| `database.type` | `String` | `"JSON"` | Persistence backend: `"JSON"`, `"SQL"`, `"MONGODB"`. |
| `buyback.enabled` | `Boolean` | `true` | Enables accidental sale buyback insurance. |
| `buyback.durationMinutes`| `Integer`| `120` | Minutes a sold Pokémon remains claimable in buyback. |
| `buyback.feePercentage` | `Double` | `10.0` | Surcharge percentage applied to repurchase. |

---

## 📋 Enterprise Production Templates

### Production `config.json` Template
```json
{
  "debug": false,
  "lang": "en",
  "database": {
    "type": "MYSQL",
    "database": "ultrasts",
    "url": "jdbc:mysql://10.0.0.5:3306/ultrasts?useSSL=false&serverTimezone=UTC",
    "user": "sts_admin",
    "password": "SecureSTSPassword456"
  },
  "buyback": {
    "enabled": true,
    "durationMinutes": 120,
    "feePercentage": 10.0
  },
  "commands": [
    "sts",
    "sellpokemon",
    "venderpokemon"
  ]
}
```
