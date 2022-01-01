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
package org.kryptonmc.krypton.network.data

import com.google.common.net.InetAddresses
import io.netty.buffer.ByteBuf
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.kryptonmc.krypton.auth.KryptonProfileProperty
import org.kryptonmc.krypton.util.readAvailableBytes
import org.kryptonmc.krypton.util.readString
import org.kryptonmc.krypton.util.readVarInt
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object VelocityProxy {

    private const val SUPPORTED_FORWARDING_VERSION = 1

    @JvmStatic
    fun verifyIntegrity(buf: ByteBuf, secret: ByteArray): Boolean {
        val signature = buf.readAvailableBytes(32)

        val data = ByteArray(buf.readableBytes())
        buf.getBytes(buf.readerIndex(), data, 0, buf.readableBytes())

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        val mySignature = mac.doFinal(data)
        if (!MessageDigest.isEqual(signature, mySignature)) return false

        val version = buf.readVarInt()
        return version == SUPPORTED_FORWARDING_VERSION
    }

    @JvmStatic
    fun readData(buf: ByteBuf): VelocityForwardedData = VelocityForwardedData(
        InetAddresses.forString(buf.readString()),
        UUID(buf.readLong(), buf.readLong()),
        buf.readString(16),
        readProperties(buf)
    )

    @JvmStatic
    private fun readProperties(buf: ByteBuf): PersistentList<KryptonProfileProperty> {
        val properties = persistentListOf<KryptonProfileProperty>().builder()
        repeat(buf.readVarInt()) {
            val name = buf.readString()
            val value = buf.readString()
            val signature = if (buf.readBoolean()) buf.readString() else null
            properties.add(KryptonProfileProperty(name, value, signature))
        }
        return properties.build()
    }
}