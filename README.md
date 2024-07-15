# RPGDamageOverhaul (RPGDO)
RPGDO is a complete overhaul to minecraft's damage system, inspired by [DDD](https://github.com/yeelp/Distinct-Damage-Descriptions) and [Alembic](https://github.com/FoundryMC/Alembic). It changes the armor scaling to be more balanced, and also adds specific armor for specific damages.
This mod is also very similar to [TheRedBrain's Overhauled Damage](https://modrinth.com/mod/overhauled-damage), but this one lets you create and remove damage classes as you wish, while his comes with a immutable set of damages.

## Defaults
By default, RPGDO comes with 10 damage classes:
  - Blunt
  - Piercing
  - Slashing
  - Physical(generic)
  - Fire
  - Frost
  - Lighting
  - Soul
  - Holy
  - Arcane
  - True

And all of those come with support for all Spell Power, and if you have [Better Combat Extension](https://modrinth.com/mod/bettercombat-extension) installed, it also comes with support for Better Combat's default movesets(for blunt piercing and slashing damage), and minecraft's default damages. These values can be checked in `src/main/resources/data/rpgdamageoverhaul/rpgdamageoverhaul`

## Damage Overrides
In highly modded enviroments, it's probable that some mods will add Damage Types that this mod does not cover by default. To solve this, you can create your own Damage Overrides at `data/namespace/rpgdamageoverhaul/damage_overrides.json`. The file follows this format:
```
{
  [damage type]: {[damage class]: multiplier, [damage class]: multiplier}
}
```
For example, in the default datapack, the warden's sonic boom deals 40% soul damage, 40% arcane damage, and 20% true damage:
```
"minecraft:sonic_boom": {"arcane": 0.4, "soul": 0.4, "true": 0.2},
```
Again, you can check the defaults in the default datapack.
## Damage Classes
All damage classes come with 4 attributes:
```
rpgdamageoverhaul:[damage name].damage
rpgdamageoverhaul:[damage name].resistance
rpgdamageoverhaul:[damage name].armor
rpgdamageoverhaul:[damage name].absorption
```
  * Damage makes it so that you deal that amount of extra damage with that damage class on hit.
  * Resistance is a percentage reduction to the damage taken by this class(1 means 100% resistance, so zero damage, -1 means 100% vulnerability, so double damage).
  * Armor is a flat reduction to the damage taken that respects the same scaling as vanilla armor values.
  * Absorption[W.I.P] is the same as the absorption effect, but only for this specific damage type.

You can define damage classes through datapacks by creating a `rpgdamageoverhaul/damage_classes.json` folder inside your datapack's namespace folder, an example path would be: `data/namespace/rpgdamageoverhaul/damage_classes.json`.
The json format is:
```
{
  [damage name]: {
    [properties]
    "subClasses": {
      [other damage classes]
    }
  },
  .....
}
```

For example, this is how physical damage is defined:
```
  "physical": {

    "subClasses": {
      "blunt": {

      },
      "piercing": {

      },
      "slashing": {

      }
    }
  }
```


For more examples, check the default datapack.

## Properties
For now, the only default properties are:
### onHit
Defines what happens when a damageclass is applied to an entity. The only default on hit effects are `rpgdamageoverhaul:set_fire`, `rpgdamageoverhaul:set_frozen`, and `rpgdamageoverhaul:particles`, which require the `particle` property. You can register your own onHitEffects through code.
### particle
If the DamageClass has the `rpgdamageoverhaul:particles` onHitEffect, it spawns this particle type. For example, in the fire damage class, you have `"particle": "minecraft:flame"`

## For Developers
You can register and get damage classes, overrides and OnHitEffects by using the `RPGDamageOverhaulAPI` class. Also, all `DamageClass` objects come with a `properties` map even if a certain property doesn't have any effect. Use this to use custom properties.
