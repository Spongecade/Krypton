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
package org.kryptonmc.krypton.entity.projectile

import org.kryptonmc.api.entity.Entity
import org.kryptonmc.api.entity.EntityTypes
import org.kryptonmc.api.entity.projectile.ShulkerBullet
import org.kryptonmc.api.util.Direction
import org.kryptonmc.krypton.world.KryptonWorld
import java.util.UUID

class KryptonShulkerBullet(world: KryptonWorld) : KryptonProjectile(world, EntityTypes.SHULKER_BULLET), ShulkerBullet {

    internal var targetId: UUID? = null
    override var steps: Int = 0
    override var target: Entity? = null
    var movingDirection: Direction? = null
    var targetDeltaX: Double = 0.0
    var targetDeltaY: Double = 0.0
    var targetDeltaZ: Double = 0.0
}
