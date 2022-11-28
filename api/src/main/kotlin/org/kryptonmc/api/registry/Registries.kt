/*
 * This file is part of the Krypton API, licensed under the MIT license.
 *
 * Copyright (C) 2021-2022 KryptonMC and the contributors to the Krypton project.
 *
 * This project is licensed under the terms of the MIT license.
 * For more details, please reference the LICENSE file in the api top-level directory.
 */
package org.kryptonmc.api.registry

import net.kyori.adventure.key.Key
import org.jetbrains.annotations.Contract
import org.kryptonmc.api.Krypton
import org.kryptonmc.api.block.Block
import org.kryptonmc.api.block.entity.BlockEntityType
import org.kryptonmc.api.block.entity.banner.BannerPatternType
import org.kryptonmc.api.effect.particle.ParticleType
import org.kryptonmc.api.effect.sound.SoundEvent
import org.kryptonmc.api.entity.EntityCategory
import org.kryptonmc.api.entity.EntityType
import org.kryptonmc.api.entity.attribute.AttributeType
import org.kryptonmc.api.entity.hanging.PaintingVariant
import org.kryptonmc.api.fluid.Fluid
import org.kryptonmc.api.inventory.InventoryType
import org.kryptonmc.api.item.ItemRarity
import org.kryptonmc.api.item.ItemType
import org.kryptonmc.api.resource.ResourceKey
import org.kryptonmc.api.resource.ResourceKeys
import org.kryptonmc.api.scoreboard.criteria.KeyedCriterion
import org.kryptonmc.api.statistic.StatisticType
import org.kryptonmc.api.util.Catalogue
import org.kryptonmc.api.world.biome.Biome
import org.kryptonmc.api.world.damage.type.DamageType
import org.kryptonmc.api.world.dimension.DimensionType
import org.kryptonmc.api.world.rule.GameRule

/**
 * Holder of all of the built-in registries.
 */
@Catalogue(Registry::class)
public object Registries {

    /**
     * All built-in vanilla registries.
     */
    @JvmField
    public val SOUND_EVENT: Registry<SoundEvent> = builtin(ResourceKeys.SOUND_EVENT)
    @JvmField
    public val FLUID: DefaultedRegistry<Fluid> = builtinDefaulted(ResourceKeys.FLUID)
    @JvmField
    public val BLOCK: DefaultedRegistry<Block> = builtinDefaulted(ResourceKeys.BLOCK)
    @JvmField
    public val ENTITY_TYPE: DefaultedRegistry<EntityType<*>> = builtinDefaulted(ResourceKeys.ENTITY_TYPE)
    @JvmField
    public val ITEM: DefaultedRegistry<ItemType> = builtinDefaulted(ResourceKeys.ITEM)
    @JvmField
    public val PARTICLE_TYPE: Registry<ParticleType> = builtin(ResourceKeys.PARTICLE_TYPE)
    @JvmField
    public val BLOCK_ENTITY_TYPE: Registry<BlockEntityType<*>> = builtin(ResourceKeys.BLOCK_ENTITY_TYPE)
    @JvmField
    public val PAINTING_VARIANT: DefaultedRegistry<PaintingVariant> = builtinDefaulted(ResourceKeys.PAINTING_VARIANT)
    @JvmField
    public val CUSTOM_STATISTIC: Registry<Key> = builtin(ResourceKeys.CUSTOM_STATISTIC)
    @JvmField
    public val INVENTORY_TYPE: Registry<InventoryType> = builtin(ResourceKeys.INVENTORY_TYPE)
    @JvmField
    public val ATTRIBUTE: Registry<AttributeType> = builtin(ResourceKeys.ATTRIBUTE)
    @JvmField
    public val STATISTIC_TYPE: Registry<StatisticType<*>> = builtin(ResourceKeys.STATISTIC_TYPE)
    @JvmField
    public val BANNER_PATTERN: Registry<BannerPatternType> = builtin(ResourceKeys.BANNER_PATTERN)
    @JvmField
    public val DIMENSION_TYPE: Registry<DimensionType> = builtin(ResourceKeys.DIMENSION_TYPE)
    @JvmField
    public val BIOME: Registry<Biome> = builtin(ResourceKeys.BIOME)

    /**
     * Custom built-in registries.
     */
    @JvmField
    public val GAME_RULES: Registry<GameRule<*>> = builtin(ResourceKeys.GAME_RULES)
    @JvmField
    public val CRITERIA: Registry<KeyedCriterion> = builtin(ResourceKeys.CRITERIA)
    @JvmField
    public val ITEM_RARITIES: Registry<ItemRarity> = builtin(ResourceKeys.ITEM_RARITIES)
    @JvmField
    public val ENTITY_CATEGORIES: Registry<EntityCategory> = builtin(ResourceKeys.ENTITY_CATEGORIES)
    @JvmField
    public val DAMAGE_TYPES: Registry<DamageType> = builtin(ResourceKeys.DAMAGE_TYPES)

    @JvmStatic
    private fun <T> builtin(key: ResourceKey<out Registry<T>>): Registry<T> =
        requireNotNull(getRegistry(key)) { "Cannot find built-in registry $key!" }

    @JvmStatic
    private fun <T> builtinDefaulted(key: ResourceKey<out Registry<T>>): DefaultedRegistry<T> =
        requireNotNull(getDefaultedRegistry(key)) { "Cannot find built-in defaulted registry $key!" }

    /**
     * Gets the existing registry with the given resource [key], or returns null
     * if there is no existing registry with the given resource [key].
     *
     * @param T the registry element type
     * @param key the key
     * @return the existing registry, or null if not present
     */
    @JvmStatic
    public fun <T> getRegistry(key: ResourceKey<out Registry<T>>): Registry<T>? = Krypton.registryManager().getRegistry(key)

    /**
     * Gets the existing defaulted registry with the given resource [key], or
     * returns null if there is no existing defaulted registry with the given
     * resource [key].
     *
     * @param T the registry element type
     * @param key the key
     * @return the existing defaulted registry, or null if not present
     */
    @JvmStatic
    public fun <T> getDefaultedRegistry(key: ResourceKey<out Registry<T>>): DefaultedRegistry<T>? =
        Krypton.registryManager().getDefaultedRegistry(key)

    /**
     * Creates a new registry with the given registry [key].
     *
     * @param T the registry element type
     * @param key the registry key
     * @return a registry for the given [key]
     */
    @JvmStatic
    @Contract("_ -> new", pure = true)
    public fun <T> create(key: ResourceKey<out Registry<T>>): Registry<T> = Krypton.registryManager().create(key)

    /**
     * Creates a new registry with the given registry [key], with a
     * [defaultKey].
     *
     * The default value for this registry will be the first value registered
     * that has a key that matches the given [defaultKey].
     *
     * @param T the registry element type
     * @param key the registry key
     * @param defaultKey the default key
     * @return a defaulted registry for the given [key]
     */
    @JvmStatic
    @Contract("_, _ -> new", pure = true)
    public fun <T> createDefaulted(key: ResourceKey<out Registry<T>>, defaultKey: Key): DefaultedRegistry<T> =
        Krypton.registryManager().createDefaulted(key, defaultKey)
}
