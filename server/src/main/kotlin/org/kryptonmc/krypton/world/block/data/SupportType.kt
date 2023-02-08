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
package org.kryptonmc.krypton.world.block.data

import org.kryptonmc.api.util.Direction
import org.kryptonmc.api.util.Vec3i
import org.kryptonmc.krypton.shapes.Shapes
import org.kryptonmc.krypton.shapes.util.BooleanOperator
import org.kryptonmc.krypton.world.block.KryptonBlock
import org.kryptonmc.krypton.world.block.state.KryptonBlockState
import org.kryptonmc.krypton.world.components.BlockGetter

enum class SupportType {

    FULL {

        override fun isSupporting(state: KryptonBlockState, world: BlockGetter, pos: Vec3i, face: Direction): Boolean =
            KryptonBlock.isFaceFull(state.getBlockSupportShape(world, pos), face)
    },
    CENTER {

        private val supportShape = KryptonBlock.box(7.0, 0.0, 7.0, 9.0, 10.0, 9.0)

        override fun isSupporting(state: KryptonBlockState, world: BlockGetter, pos: Vec3i, face: Direction): Boolean =
            !Shapes.joinIsNotEmpty(state.getBlockSupportShape(world, pos).getFaceShape(face), supportShape, BooleanOperator.ONLY_SECOND)
    },
    RIGID {

        private val supportShape = Shapes.join(Shapes.block(), KryptonBlock.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0), BooleanOperator.ONLY_FIRST)

        override fun isSupporting(state: KryptonBlockState, world: BlockGetter, pos: Vec3i, face: Direction): Boolean =
            !Shapes.joinIsNotEmpty(state.getBlockSupportShape(world, pos).getFaceShape(face), supportShape, BooleanOperator.ONLY_FIRST)
    };

    abstract fun isSupporting(state: KryptonBlockState, world: BlockGetter, pos: Vec3i, face: Direction): Boolean
}
