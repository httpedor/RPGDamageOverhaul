# RPGDamageOverhaul (RPGDO)

RPGDO is a complete overhaul of Minecraft's damage system, inspired by
[DDD](https://github.com/yeelp/Distinct-Damage-Descriptions) and
[Alembic](https://github.com/FoundryMC/Alembic). It rebalances armor scaling and
adds per‑damage‑class armor, resistance, bonus‑damage and absorption attributes.

It is similar to [TheRedBrain's Overhauled Damage](https://modrinth.com/mod/overhauled-damage),
but here every damage class is data‑driven: you can add, remove or re‑parent
classes and change their behaviour entirely from a datapack.

* Minecraft **1.21.1**, **NeoForge** and **Fabric** (Fabric API required on Fabric).
* No hard mod dependencies. Optional integration with Better Combat, Iron's
  Spellbooks, Spell Power, [AttributeSetter](https://modrinth.com/mod/attributesetter)
  and Jade, plus bundled compat data for many mob/weapon mods.

> **Migrating from 1.20.1:** the datapack format changed. Damage classes are now
> one file per class under `rpgdamageoverhaul/damage_class/`, the old nested
> `subClasses` tree is gone (use the `parent` property), `damage_classes.json` is
> no longer read, and several property names changed (see the tables below).

---

## Defaults

RPGDO ships with these damage classes (indentation = parent → child):

```
physical            generic melee
├─ blunt
└─ sharp
   ├─ piercing
   └─ slashing
elemental           umbrella for the magical/elemental types
├─ lava
│  └─ fire
├─ frost
│  └─ water
├─ lightning
├─ poison
├─ blood
├─ wither
├─ holy
└─ soul
arcane
true                ignores armor, resistance, enchantments and effects
```

Vanilla damage types, mob attacks and tool damage are mapped onto these classes by
the bundled datapack (`common/src/main/resources/data/rpgdamageoverhaul/`). If
Better Combat Extension is installed, its default movesets are mapped too, and if
Spell Power / Iron's Spellbooks are present their spell schools are wired into the
matching classes.

---

## Attributes

Every damage class `X` gets four attributes:

| Attribute | Range | Meaning |
|---|---|---|
| `rpgdamageoverhaul:X.damage` | `0 … 1024` | Flat extra damage of class `X` dealt on hit. |
| `rpgdamageoverhaul:X.resistance` | `-10 … 10` | Fractional reduction of incoming `X` damage. `1` = immune, `0.5` = half damage, `0` = normal, `-1` = double damage. |
| `rpgdamageoverhaul:X.armor` | `0 … 1024` | Flat reduction of incoming `X` damage, using the vanilla armor/toughness formula. |
| `rpgdamageoverhaul:X.absorption` | `0 … 1024` | Per‑class absorption pool, like the Absorption effect but only for class `X`. **(work in progress)** |

Resistance and armor from a parent class also protect against its children,
scaled by `parent_defense_effectiveness` / `parent_damage_defense_effectiveness`
(see the property table).

---

## Datapack files

All of these live under `data/<namespace>/rpgdamageoverhaul/` and are applied on
world load and on `/reload`. Files from different datapacks/namespaces are merged.

| Path | Purpose |
|---|---|
| `rpgdamageoverhaul/damage_class/<class>.json` | Defines one damage class. The **file name is the class name**. |
| `rpgdamageoverhaul/damage_overrides.json` | Splits a vanilla/modded **damage type** into classes. |
| `rpgdamageoverhaul/item_overrides.json` | Sets what classes an **item's** attack deals. |
| `rpgdamageoverhaul/entity_overrides.json` | Sets what classes a **mob's** melee attack deals. |
| `rpgdamageoverhaul/bettercombat.json` | Per‑attack class list for **Better Combat** weapon movesets. |

### Damage type overrides — `damage_overrides.json`

```json
{
  "<damage type id>": { "<class>": <multiplier>, "<class>": <multiplier> }
}
```

The incoming damage is divided among the listed classes by multiplier. Example
(the warden's sonic boom is 40% soul, 40% arcane, 20% true):

```json
"minecraft:sonic_boom": { "soul": 0.4, "arcane": 0.4, "true": 0.2 }
```

### Item overrides — `item_overrides.json`

```json
{
  "<item id or #tag>": { "<class>": <multiplier>, ... }
}
```

The item's attack damage is split among the classes. Example — every sword deals
80% slashing / 20% piercing:

```json
"#c:swords": { "slashing": 0.8, "piercing": 0.2 }
```

### Entity overrides — `entity_overrides.json`

Same shape as item overrides, keyed by entity type id or `#entity_type_tag`.
Applied to a mob's own melee attacks. Example:

```json
"minecraft:ravager": { "blunt": 0.66, "piercing": 0.33 },
"#minecraft:skeletons": { "blunt": 1 }
```

### Better Combat overrides — `bettercombat.json`

Keyed by Better Combat attack id; the value is a class per attack in the moveset.
RPGDO also derives a fallback item override from the list.

```json
"bettercombat:dagger": ["slashing", "slashing", "piercing"]
```

---

## Defining a damage class

Create `data/<namespace>/rpgdamageoverhaul/damage_class/<name>.json`. The file is a
flat JSON object of properties (no `subClasses` nesting anymore). Minimal example:

```json
{
  "parent": "physical",
  "color": "GRAY"
}
```

If two datapacks define the same class, the files are merged in ascending
`"priority"` order (default `0`); same‑type properties are merged, others are
overwritten by the higher priority file.

### Properties

Numeric fields marked *formula* accept either a plain number (used as a
multiplier) or an [EvalEx](https://github.com/uklimaschewski/EvalEx) expression
string (`MIN`, `MAX`, `IF`, …). The variable available is `dmg` (the damage dealt
this hit) unless noted otherwise.

#### Identity / hierarchy

| Property | Type | Description |
|---|---|---|
| `parent` | string | Name of the parent class. Omit for a root class. |
| `parent_defense_effectiveness` | number | How much of a parent's resistance/armor this class inherits (`1` = fully). |
| `parent_damage_defense_effectiveness` | number | How much this class's resistance/armor protects against its **children** (default `1`). |
| `color` | string | Named color (`RED`, `DARK_PURPLE`, …) or hex (`#3F76E4`). Used in tooltips. |
| `armor_effectiveness` | formula(`dmg`, `armor`) | Rescales the effective armor value against this class. `0` = armor ignored. |
| `damage_types` | string[] | Vanilla/modded damage type ids that should be treated as this class. |
| `tags` | string[] | Damage type tag ids this class's damage type should belong to (e.g. `minecraft:bypasses_armor`). |
| `rename_attributes` | `{ "<attr id>": "damage"\|"resistance"\|"armor"\|"absorption" }` | Treats another mod's attribute as this class's attribute for tooltip display. |

#### Attribute scaling

| Property | Shape | Description |
|---|---|---|
| `potions` | `{ "<slot>": { "<mob effect id>": <amount or {operation,value}> } }` | While the entity has the effect, adds `amount` to the class's `damage`/`resistance`/`armor`/`absorption` attribute (`<slot>`). |
| `enchantments` | `{ "<slot>": { "<enchantment id>": <per‑level formula(`lvl`)> } }` | Adds `value(level)` to the class attribute for each level of the enchantment held/worn. |

#### On‑hit effects (run when this class lands on a `LivingEntity`)

| Property | Shape | Description |
|---|---|---|
| `particle` | string | Spawns this (simple) particle type on the target. |
| `ignite` | number (seconds) | Sets the target on fire. |
| `freeze` | number (seconds) | Applies powder‑snow freezing. |
| `set_wet` | formula(`dmg`) → seconds | Marks the target "wet" (feeds `on_water`). |
| `on_water` | formula(`dmg`) | Damage multiplier applied while the target is wet or in rain. |
| `heal_on_hit` | formula(`dmg`) | Heals the attacker. |
| `apply_potion` | `{ "<mob effect id>": { "duration": formula(`dmg`), "amplifier": formula(`dmg`) } }` | Applies a status effect (duration in seconds). |
| `modify_damage` | `{ "modifier": formula(`dmg`), "duration": formula(`dmg`), "exceptions": [..] \| "whitelist": [..] }` | Temporarily buffs/debuffs damage the target then takes. `exceptions` excludes classes; `whitelist` restricts to classes. |
| `modify_heal` | `{ "modifier": formula(`heal`,`dmg`), "duration": formula(`dmg`) }` | Temporarily scales the target's incoming healing (anti‑heal). |
| `chain_damage` | `{ "max_targets": formula(`dmg`), "range": formula(`dmg`), "damage_modifier": number \| formula(`dmg`,`n`) }` | Arcs the hit to nearby entities. With a number, damage is `dmg * modifier^n`. |
| `stacking` | `{ "max_stacks": int, "stack_duration": seconds, "stack_damage": formula(`dmg`) }` | Builds stacks on the target that deal periodic damage. |
| `attribute_modifier` (alias `attribute_modifiers`) | `[ { "attribute": id, "operation": ADD_VALUE\|ADD_MULTIPLIED_BASE\|ADD_MULTIPLIED_TOTAL, "amount": formula(`dmg`), "duration": formula(`dmg`), "replace_type": ALWAYS\|LOWER\|HIGHER } ]` | Applies timed attribute modifiers to the target. `replace_type` decides whether a new modifier overrides an existing one of the same id. |

> `copy_attributes` / `inject_attributes` appear in the bundled class files but are
> not implemented yet — they are ignored at load time.

### Example — the `fire` class

```json
{
  "parent": "lava",
  "parent_defense_effectiveness": 1.75,
  "parent_damage_defense_effectiveness": 0.5,
  "particle": "minecraft:flame",
  "ignite": 0.5,
  "color": "RED",
  "on_water": 0.5,
  "damage_types": ["minecraft:lava"],
  "tags": ["minecraft:is_fire"],
  "potions": {
    "resistance": { "minecraft:fire_resistance": 0.8 }
  },
  "enchantments": {
    "damage": { "minecraft:fire_aspect": 2 },
    "resistance": { "minecraft:fire_protection": 0.04 }
  },
  "stacking": { "stack_duration": 2, "max_stacks": 3, "stack_damage": "dmg/3" }
}
```

---

## For developers

`com.httpedor.rpgdamageoverhaul.api.RPGDamageOverhaulAPI` is the entry point for
registering/looking up damage classes, overrides and property builders:

* `getDamageClass`, `getAllDamageClasses`, `getDamageOverrides`, `getItemOverrides`,
  `getEntityOverrides`.
* `registerPropertyBuilder(name, jsonBuilder[, defaultSupplier])` and
  `registerPropertyAlias(name, alias)` — add your own class properties. A property
  implementing `OnHitProperty` runs on every landed hit of its class.
* `DamageClass#createDamageSource(...)` — build a `DamageSource` for a class so
  your own attacks go through the RPGDO pipeline.

A `DamageClassRegisteredEvent` (NeoForge) / `DamageClassRegisteredCallback`
(Fabric) fires for each class as it is (re)loaded.

---

## Building

This is a [MultiLoader](https://github.com/jaredlll08/MultiLoader-Template) project:
shared code lives in `common/`, loader‑specific code in `fabric/` and `neoforge/`.
Requires JDK 21. Import the root folder as a Gradle project; the `runClient` /
`runServer` tasks exist for both loaders.
