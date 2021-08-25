/*
 * This file is part of the Krypton project, licensed under the GNU General Public License v3.0
 *
 * Copyright (C) 2021 KryptonMC and the contributors of the Krypton project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.kryptonmc.krypton.world.dimension

import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Key.key
import org.kryptonmc.api.registry.Registries
import org.kryptonmc.api.resource.ResourceKey
import org.kryptonmc.krypton.registry.InternalRegistries
import org.kryptonmc.krypton.registry.InternalResourceKeys
import org.kryptonmc.krypton.tags.BlockTags
import org.kryptonmc.krypton.world.biome.gen.BiomeZoomer
import org.kryptonmc.krypton.world.biome.gen.FuzzyOffsetBiomeZoomer
import org.kryptonmc.krypton.world.biome.gen.FuzzyOffsetConstantColumnBiomeZoomer

object DimensionTypes {

    private val OVERWORLD_EFFECTS = key("overworld")
    private val THE_NETHER_EFFECTS = key("the_nether")
    private val THE_END_EFFECTS = key("the_end")

    val OVERWORLD = register(
        false,
        true,
        false,
        true,
        false,
        true,
        true,
        false,
        0F,
        null,
        BlockTags.INFINIBURN_OVERWORLD.name,
        0,
        256,
        256,
        1.0,
        OVERWORLD_EFFECTS,
        FuzzyOffsetConstantColumnBiomeZoomer
    )
    val OVERWORLD_CAVES = register(
        false,
        true,
        false,
        true,
        true,
        true,
        true,
        false,
        0F,
        null,
        BlockTags.INFINIBURN_OVERWORLD.name,
        0,
        256,
        256,
        1.0,
        OVERWORLD_EFFECTS,
        FuzzyOffsetConstantColumnBiomeZoomer
    )
    val THE_NETHER = register(
        true,
        false,
        true,
        false,
        true,
        false,
        false,
        true,
        0.1F,
        18000L,
        BlockTags.INFINIBURN_NETHER.name,
        0,
        256,
        128,
        8.0,
        THE_NETHER_EFFECTS,
        FuzzyOffsetBiomeZoomer
    )
    val THE_END = register(
        false,
        false,
        false,
        false,
        false,
        true,
        false,
        false,
        0F,
        6000L,
        BlockTags.INFINIBURN_END.name,
        0,
        256,
        256,
        1.0,
        THE_END_EFFECTS,
        FuzzyOffsetBiomeZoomer
    )

    private fun register(
        isPiglinSafe: Boolean,
        isNatural: Boolean,
        isUltrawarm: Boolean,
        hasSkylight: Boolean,
        hasCeiling: Boolean,
        hasRaids: Boolean,
        bedWorks: Boolean,
        respawnAnchorWorks: Boolean,
        ambientLight: Float,
        fixedTime: Long?,
        infiniburn: Key,
        minimumY: Int,
        height: Int,
        logicalHeight: Int,
        coordinateScale: Double,
        effects: Key,
        biomeZoomer: BiomeZoomer = FuzzyOffsetBiomeZoomer
    ) = Registries.register(
        InternalRegistries.DIMENSION_TYPE,
        effects,
        KryptonDimensionType(
            isPiglinSafe,
            isNatural,
            isUltrawarm,
            hasSkylight,
            hasCeiling,
            hasRaids,
            bedWorks,
            respawnAnchorWorks,
            ambientLight,
            fixedTime,
            infiniburn,
            minimumY,
            height,
            logicalHeight,
            coordinateScale,
            effects,
            biomeZoomer
        )
    )
}
