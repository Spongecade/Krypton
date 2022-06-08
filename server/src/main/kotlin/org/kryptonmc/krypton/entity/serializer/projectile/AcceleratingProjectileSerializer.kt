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
package org.kryptonmc.krypton.entity.serializer.projectile

import org.kryptonmc.krypton.entity.projectile.KryptonAcceleratingProjectile
import org.kryptonmc.krypton.entity.serializer.EntitySerializer
import org.kryptonmc.nbt.CompoundTag
import org.kryptonmc.nbt.DoubleTag
import org.kryptonmc.nbt.ListTag
import org.spongepowered.math.vector.Vector3d

object AcceleratingProjectileSerializer : EntitySerializer<KryptonAcceleratingProjectile> {

    override fun load(entity: KryptonAcceleratingProjectile, data: CompoundTag) {
        ProjectileSerializer.load(entity, data)
        if (!data.contains("power", ListTag.ID)) return
        val power = data.getList("power", DoubleTag.ID)
        if (power.size != 3) return
        entity.acceleration = Vector3d(power.getDouble(0), power.getDouble(1), power.getDouble(2))
    }

    override fun save(entity: KryptonAcceleratingProjectile): CompoundTag.Builder = ProjectileSerializer.save(entity).apply {
        list("power") {
            addDouble(entity.acceleration.x())
            addDouble(entity.acceleration.y())
            addDouble(entity.acceleration.z())
        }
    }
}