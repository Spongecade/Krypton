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
package org.kryptonmc.krypton.packet.out.play.data

import io.netty.buffer.ByteBuf
import kotlinx.collections.immutable.persistentListOf
import org.kryptonmc.krypton.network.Writable
import org.kryptonmc.krypton.util.writeLongArray
import org.kryptonmc.krypton.util.writeVarInt
import org.kryptonmc.krypton.world.chunk.KryptonChunk
import java.util.BitSet

@JvmRecord
data class LightPacketData(
    val trustEdges: Boolean,
    val skyMask: BitSet,
    val blockMask: BitSet,
    val emptySkyMask: BitSet,
    val emptyBlockMask: BitSet,
    val skyLights: List<ByteArray>,
    val blockLights: List<ByteArray>
) : Writable {

    override fun write(buf: ByteBuf) {
        buf.writeBoolean(trustEdges)

        buf.writeLongArray(skyMask.toLongArray())
        buf.writeLongArray(blockMask.toLongArray())
        buf.writeLongArray(emptySkyMask.toLongArray())
        buf.writeLongArray(emptyBlockMask.toLongArray())

        buf.writeVarInt(skyLights.size)
        for (i in skyLights.indices) {
            val light = skyLights[i]
            buf.writeVarInt(light.size)
            buf.writeBytes(light)
        }

        buf.writeVarInt(blockLights.size)
        for (i in blockLights.indices) {
            val light = blockLights[i]
            buf.writeVarInt(light.size)
            buf.writeBytes(light)
        }
    }

    companion object {

        @JvmStatic
        fun create(chunk: KryptonChunk, trustEdges: Boolean): LightPacketData {
            val sections = chunk.sections

            val skyMask = BitSet()
            val blockMask = BitSet()
            val emptySkyMask = BitSet()
            val emptyBlockMask = BitSet()
            val skyLights = persistentListOf<ByteArray>().builder()
            val blockLights = persistentListOf<ByteArray>().builder()

            for (i in sections.indices) {
                val section = sections[i]
                if (section.hasOnlyAir()) {
                    emptySkyMask.set(i)
                    emptyBlockMask.set(i)
                    continue
                }

                // Deal with sky light data
                if (hasNonZeroData(section.skyLight)) {
                    skyMask.set(i)
                    skyLights.add(section.skyLight)
                } else {
                    emptySkyMask.set(i)
                }

                // Deal with block light data
                if (hasNonZeroData(section.blockLight)) {
                    blockMask.set(i)
                    blockLights.add(section.blockLight)
                } else {
                    emptyBlockMask.set(i)
                }
            }

            return LightPacketData(trustEdges, skyMask, blockMask, emptySkyMask, emptyBlockMask, skyLights, blockLights)
        }

        @JvmStatic
        private fun hasNonZeroData(array: ByteArray): Boolean {
            for (i in array.indices) {
                if (array[i] != 0.toByte()) return true
            }
            return false
        }
    }
}