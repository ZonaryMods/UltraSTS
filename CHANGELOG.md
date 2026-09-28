# Changelog

## [1.6.4] - 2026-09-28

### Fixed

- **Custom Profile & Leaderboard Icons**: Fixed an issue where custom items assigned to the profile button in the main menu or player icons in the leaderboard were ignored and always displayed as player heads. Custom icons are now properly respected.
- **Held Item Detection**: Fixed an issue where Pokémon holding allowed items could be blocked from sale because of the way items were identified. Also fixed the default configuration so it no longer accidentally blocks all held items out of the box.
- **Menu Memory Cleanup**: Fixed a memory issue where Pokémon selections from open STS menus were kept in memory after a player disconnected. Sessions are now cleared immediately upon logout.
- **Leaderboard Crash & Startup Safety**: Fixed a potential crash when querying STS categories if none were loaded or configured, making menu lookups safe against missing data.
- **Reset Command Feedback & Console Support**: The `/sts reset` command can now be safely used from the server console (specifying a player name) and now sends clear confirmation messages when cooldowns are reset.
- **Notification Reminders for New Players**: Fixed an issue where new players who hadn't made any sales yet never received reminders about available STS categories.

### Changed

- **Translations for Reset Command**: Added new translatable messages to `lang` for cooldown resets and player-not-found notices (`resetSuccess`, `resetSuccessOther`, `playerNotFound`).

## [1.6.3] - 2026-09-14

### Fixed

- **Menu Descriptions & Lore**: Fixed an issue where category descriptions and lores were missing in the leaderboard selection menu.
- **Leaderboard Display**: Player ranking and money earned in the leaderboard are now formatted properly with the category's currency.
- **Language & Translations**: Replaced remaining hardcoded menu titles and fixed text formatting across menus to ensure full translation support.
- **Menu Stability**: Added safety checks to prevent crashes when opening menus with incomplete or custom configuration files.

## [1.6.2] - 2026-09-13

### Fixed

- **Thread-Safety in Pokemon Sales**: Fixed concurrency issue where selling Pokémon from party or PC could run asynchronously instead of safely on the main server thread.
- **Concurrent Session Selection**: Improved thread-safe Pokémon selection during STS menus and multi-selection.
## [1.6.1] - 2026-09-11

### Changed

- **Cobblemon 1.8.0 Compatibility**: Updated to support Cobblemon 1.8.0 on Minecraft 1.21.1.

### Fixed

- **MongoDB Stability**: Fixed an issue where player data could fail to load when joining after a database reconnection or server reload.


**IMPORTANT:** Please test the changes listed below to ensure they work correctly.

## [1.6.0] - 2026-07-14

### Added

- Added dynamic category permission checking and display:
    - If a user lacks permission for a category, the error message now indicates which permission is required.
    - Supports both the default permission (`ultrasts.join.<id>`) and custom permissions set via the category's `permission` field.
- Added translatable strings to `Lang.java` (generating `lang/en_us.json` automatically):
    - `noPermissionCategory`: Custom error message for category permission failures (with `%permission%` placeholder).
    - `cannotJoinCategory`, `errorSellingPokemon`, `cannotSellPokemon`: Error messages for single-pokemon selling menu.
    - `noPokemonSelected`, `errorProcessingSale`, `noValidPokemonSold`: Error messages for multi-pokemon selling menu.
    - `errorLoadingProfile`: Error message when failing to load user profiles in GUI menus.
    - `reloadSuccess`: Custom reload command success message.
    - `unknownUser`: Fallback for unknown user names in the leaderboard.

### Changed

- Replaced all hardcoded string literals inside menus (`STSMenu`, `STSSellMenu`, `STSCategoryMenu`, `ProfileMenu`, `LeaderBoardMenu`) and commands (`ReloadCommand`) with their corresponding fields in the language configuration.

## [1.5.4] - 2026-07-06

### Fixed

- Fixed a crash during SQL database initialization where MySQL/MariaDB connections threw `SQL String cannot be empty` because index queries were empty (now checks if query is blank/empty before executing).

## [1.5.3] - 2026-06-18

### Added

- Added **leaderboard caching with 45-second TTL** for improved performance across all database types.
    - Cache is automatically invalidated when user data is saved.
    - Significantly reduces load on MongoDB, JSON file system, and SQL databases.

### Improved

- **Leaderboard rendering**: Added null-safety checks to prevent crashes from corrupted user data.
- **MongoDB leaderboard**: Improved error handling with try-catch in document conversion and better logging.
- **JSON leaderboard**: Added null validation for corrupted files and improved async processing for large user datasets.
- **SQL leaderboard**: Changed error level to WARN for parse failures to reduce log noise during normal operations.
- **Error handling**: All backend database clients now log warnings instead of errors for recoverable parsing failures.

### Bug Fixes

- Fixed potential NullPointerException in leaderboard menu when rendering corrupted user data.
- Fixed leaderboard cache not being invalidated after batch user saves.
- Fixed potential crash in LeaderBoardMenu.getButton() when moneyEarned data is missing.
- Fixed JSON database client filtering out users with null moneyGained map.

### Changed

- Leaderboard queries now use centralized caching layer to reduce database load.
- Cache invalidation is now automatic on all user save operations (single and batch).

## [1.5.1] - 2026-05-10

### Added

- Added the `/sts other <player> <sts>` command, allowing admins to open another player's STS menu.
- Added a button to clear all selected Pokémon in the multi-selection system.
- Selected Pokémon are now automatically cleared when the STS menu is closed.
    - This only removes the current selection state.
    - Pokémon are **not** removed from the player's party or PC storage.

## [1.5.0] - 2026-03-11

### Added

- Added **Banned Items Check**:
    - When **multi-selection** is enabled, the system now checks if any of the selected Pokémon are holding banned items
      before confirming the sale.
    - Added `itemBannedMessage` to the language configuration.
- Added the ability to configure **multiple STS**, where each STS can give a different **currency type and amount**.
- Added a **leaderboard**.
- Added `%lorepokemon%` placeholder to the Pokémon lore in the language configuration, allowing full Pokémon lore to be
  displayed in the STS menu.
  displayed in the STS menu.
- Added **Multi-Selection System** for selling Pokémon:
    - Users can now select multiple Pokémon from their **Party** or **PC** boxes.

### Improved

- Added **performance optimizations**.
- Improved the **save system**, so it now only saves when necessary.
- **Cooldown duration** is now **permission-based**: uses `PlayerUtils.getCooldown` with configurable
  `cooldownPermissions` per STS instead of a hardcoded 5-minute value.
- Refactored `sellPokemon` to pass the player context, ensuring cooldown permissions are evaluated correctly at sell
  time.
  `cooldownPermissions` per STS instead of a hardcoded 5-minute value.
- Refactored `sellPokemon` to pass the player context, ensuring cooldown permissions are evaluated correctly at sell
  time.

### Bug Fixes

- Fixed the `/sts reload` command — it now requires **admin permission** (`ultrasts.admin`, level 2).
- Fixed the leaderboard so it now displays the **ranking position correctly** — the position index was not incrementing
  between entries.
  between entries.
- Fixed the `/sts other` command permission level (corrected from level 4 to level 2).
- Fixed cooldown permissions not being applied when selling a Pokémon.

### Changed

- Users can now **disable the STS available notification individually**.


