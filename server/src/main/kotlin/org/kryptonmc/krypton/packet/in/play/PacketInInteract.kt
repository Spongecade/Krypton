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
package org.kryptonmc.krypton.packet.`in`.play

import io.netty.buffer.ByteBuf
import org.kryptonmc.api.entity.Hand
import org.kryptonmc.krypton.network.Writable
import org.kryptonmc.krypton.packet.Packet
import org.kryptonmc.krypton.util.ByteBufReader
import org.kryptonmc.krypton.util.readEnum
import org.kryptonmc.krypton.util.readVarInt
import org.kryptonmc.krypton.util.writeEnum
import org.kryptonmc.krypton.util.writeVarInt

@JvmRecord
data class PacketInInteract(val entityId: Int, val action: Action, val sneaking: Boolean) : Packet {

    constructor(buf: ByteBuf) : this(buf, buf.readVarInt(), buf.readEnum<ActionType>())

    private constructor(buf: ByteBuf, entityId: Int, type: ActionType) : this(entityId, type.read(buf), buf.readBoolean())

    override fun write(buf: ByteBuf) {
        buf.writeVarInt(entityId)
        buf.writeEnum(action.type)
        action.write(buf)
        buf.writeBoolean(sneaking)
    }

    sealed interface Action : Writable {

        val type: ActionType

        fun handle(handler: Handler)
    }

    @JvmRecord
    data class InteractAction(val hand: Hand) : Action {

        override val type: ActionType
            get() = ActionType.INTERACT

        constructor(buf: ByteBuf) : this(buf.readEnum<Hand>())

        override fun handle(handler: Handler) {
            handler.onInteract(hand)
        }

        override fun write(buf: ByteBuf) {
            buf.writeEnum(hand)
        }
    }

    object AttackAction : Action {

        override val type: ActionType
            get() = ActionType.ATTACK

        override fun handle(handler: Handler) {
            handler.onAttack()
        }

        override fun write(buf: ByteBuf) {
            // Nothing to write for the attack action
        }
    }

    @JvmRecord
    data class InteractAtAction(val x: Float, val y: Float, val z: Float, val hand: Hand) : Action {

        override val type: ActionType
            get() = ActionType.INTERACT_AT

        constructor(buf: ByteBuf) : this(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readEnum<Hand>())

        override fun handle(handler: Handler) {
            handler.onInteractAt(hand, x, y, z)
        }

        override fun write(buf: ByteBuf) {
            buf.writeFloat(x)
            buf.writeFloat(y)
            buf.writeFloat(z)
            buf.writeEnum(hand)
        }
    }

    interface Handler {

        fun onInteract(hand: Hand)

        fun onInteractAt(hand: Hand, x: Float, y: Float, z: Float)

        fun onAttack()
    }

    enum class ActionType(private val reader: ByteBufReader<Action>) {

        INTERACT(::InteractAction),
        ATTACK({ AttackAction }),
        INTERACT_AT(::InteractAtAction);

        fun read(buf: ByteBuf): Action = reader.read(buf)
    }
}
