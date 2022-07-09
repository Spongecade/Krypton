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
package org.kryptonmc.krypton.packet.out.play

import io.netty.buffer.ByteBuf
import org.kryptonmc.krypton.entity.KryptonEntity
import org.kryptonmc.krypton.packet.EntityPacket
import org.kryptonmc.krypton.util.Positioning
import org.kryptonmc.krypton.util.readVarInt
import org.kryptonmc.krypton.util.writeVarInt
import org.spongepowered.math.vector.Vector3d

@JvmRecord
data class PacketOutSetEntityVelocity(override val entityId: Int, val x: Int, val y: Int, val z: Int) : EntityPacket {

    constructor(entityId: Int, velocity: Vector3d) : this(entityId, velocity.x().encode(), velocity.y().encode(), velocity.z().encode())

    constructor(
        entity: KryptonEntity,
        velocity: Vector3d = entity.velocity
    ) : this(entity.id, velocity.x().encode(), velocity.y().encode(), velocity.z().encode())

    constructor(buf: ByteBuf) : this(buf.readVarInt(), buf.readShort().toInt(), buf.readShort().toInt(), buf.readShort().toInt())

    override fun write(buf: ByteBuf) {
        buf.writeVarInt(entityId)
        buf.writeShort(x)
        buf.writeShort(y)
        buf.writeShort(z)
    }
}

private fun Double.encode(): Int = Positioning.encodeVelocity(this)