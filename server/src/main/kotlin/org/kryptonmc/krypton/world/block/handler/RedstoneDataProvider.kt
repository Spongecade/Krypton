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
package org.kryptonmc.krypton.world.block.handler

import org.kryptonmc.api.util.Direction
import org.kryptonmc.api.util.Vec3i
import org.kryptonmc.krypton.world.KryptonWorld
import org.kryptonmc.krypton.world.block.state.KryptonBlockState
import org.kryptonmc.krypton.world.components.BlockGetter

interface RedstoneDataProvider {

    fun isApplicableBlockType(name: String): Boolean

    fun isSignalSource(state: KryptonBlockState): Boolean {
        return false
    }

    fun hasAnalogOutputSignal(state: KryptonBlockState): Boolean {
        return false
    }

    fun getAnalogOutputSignal(state: KryptonBlockState, world: KryptonWorld, pos: Vec3i): Int {
        return 0
    }

    fun getSignal(state: KryptonBlockState, world: BlockGetter, pos: Vec3i, direction: Direction): Int {
        return 0
    }

    fun getDirectSignal(state: KryptonBlockState, world: BlockGetter, pos: Vec3i, direction: Direction): Int {
        return 0
    }
}
