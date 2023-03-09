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
package org.kryptonmc.krypton.world.chunk.flag

object SetBlockFlag {

    const val UPDATE_NEIGHBOURS: Int = 1
    const val NOTIFY_CLIENTS: Int = 2
    const val UPDATE_NEIGHBOUR_SHAPES: Int = 16
    const val NEIGHBOUR_DROPS: Int = 32
    const val BLOCK_MOVING: Int = 64
    const val LIGHTING: Int = 128

    const val UPDATE_NOTIFY: Int = UPDATE_NEIGHBOURS or NOTIFY_CLIENTS
    const val NO_NEIGHBOUR_DROPS: Int = NEIGHBOUR_DROPS.inv()
    const val ALL: Int = UPDATE_NEIGHBOURS or NOTIFY_CLIENTS or UPDATE_NEIGHBOUR_SHAPES or NEIGHBOUR_DROPS or BLOCK_MOVING or LIGHTING
}