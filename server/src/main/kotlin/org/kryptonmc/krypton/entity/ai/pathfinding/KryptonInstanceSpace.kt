/*
 * This file is part of the Krypton project, licensed under the GNU General Public License v3.0
 *
 * Copyright (C) 2021-2022 KryptonMC and the contributors of the Krypton project
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
package org.kryptonmc.krypton.entity.ai.pathfinding

import com.extollit.gaming.ai.path.model.IBlockObject
import com.extollit.gaming.ai.path.model.IColumnarSpace
import com.extollit.gaming.ai.path.model.IInstanceSpace
import org.kryptonmc.krypton.world.KryptonWorld
import org.kryptonmc.krypton.world.chunk.KryptonChunk
import java.util.concurrent.ConcurrentHashMap

class KryptonInstanceSpace(private val world: KryptonWorld) : IInstanceSpace {

    private val chunkSpaceMap = ConcurrentHashMap<KryptonChunk, KryptonColumnarSpace>()

    override fun blockObjectAt(x: Int, y: Int, z: Int): IBlockObject = KryptonHydrazineBlock.get(world.getBlock(x, y, z))

    override fun columnarSpaceAt(cx: Int, cz: Int): IColumnarSpace? {
        val chunk = world.getChunk(cx, cz) ?: return null
        return chunkSpaceMap.computeIfAbsent(chunk) { KryptonColumnarSpace(this, it) }
    }
}