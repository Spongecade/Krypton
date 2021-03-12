package org.kryptonmc.krypton.extension

import io.netty.buffer.ByteBuf
import io.netty.handler.codec.DecoderException
import io.netty.handler.codec.EncoderException
import net.kyori.adventure.nbt.BinaryTagIO
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.kryptonmc.krypton.entity.*
import org.kryptonmc.krypton.space.Angle
import org.kryptonmc.krypton.space.Rotation
import org.kryptonmc.krypton.entity.entities.data.VillagerData
import org.kryptonmc.krypton.entity.metadata.Optional
import org.kryptonmc.krypton.api.registry.NamespacedKey
import org.kryptonmc.krypton.api.space.Vector
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration
import java.util.*
import kotlin.experimental.and

// Allows us to write a byte without having to convert it to an integer every time
fun ByteBuf.writeByte(byte: Byte) {
    writeByte(byte.toInt())
}

fun ByteBuf.writeUByte(byte: UByte) {
    writeByte(byte.toInt())
}

fun ByteBuf.writeShort(short: Short) {
    writeShort(short.toInt())
}

fun ByteBuf.readVarInt(): Int {
    var numRead = 0
    var result = 0
    var read: Byte
    do {
        read = readByte()
        val value = (read and 127).toInt()
        result = result or (value shl 7 * numRead)
        numRead++
        if (numRead > 5) {
            throw RuntimeException("VarInt is too big")
        }
    } while (read and 128.toByte() != 0.toByte())

    return result
}

fun ByteBuf.writeVarInt(varInt: Int) {
    var i = varInt
    while (i and -128 != 0) {
        writeByte(i and 127 or 128)
        i = i ushr 7
    }

    writeByte(i)
}

fun ByteBuf.writeVarLong(varLong: Long) {
    var i = varLong
    while (i and -128 != 0.toLong()) {
        writeByte((i and 127 or 128).toInt())
        i = i ushr 7
    }

    writeByte(i.toInt())
}

fun ByteBuf.readString(maxLength: Short = Short.MAX_VALUE): String {
    val length = readVarInt()

    return when {
        length > maxLength * 4 -> {
            throw IOException("The received encoded string buffer length is longer than maximum allowed (" + length + " > " + maxLength * 4 + ")")
        }
        length < 0 -> {
            throw IOException("The received encoded string buffer length is less than zero! Weird string!")
        }
        else -> {
            val array = ByteArray(length)
            this.readBytes(array)
            val s = String(array, UTF_8)
            if (s.length > maxLength) {
                throw IOException("The received string length is longer than maximum allowed ($length > $maxLength)")
            } else {
                s
            }
        }
    }
}

fun ByteBuf.writeString(string: String, maxLength: Short = Short.MAX_VALUE) {
    val bytes = string.toByteArray(Charsets.UTF_8)
    if (bytes.size > maxLength) {
        throw EncoderException("String too big (was " + bytes.size + " bytes encoded, max " + maxLength + ")")
    } else {
        writeVarInt(bytes.size)
        writeBytes(bytes)
    }
}

fun ByteBuf.readVarIntByteArray(): ByteArray {
    val length = readVarInt()
    val readable = readableBytes()
    if (length > readable) {
        throw DecoderException("Not enough bytes to read. Expected Length: $length, actual length: $readable")
    }
    val bytes = ByteArray(length)
    readBytes(bytes)
    return bytes
}

fun ByteBuf.readAvailableBytes(length: Int): ByteArray {
    if (hasArray()) return readBytes(length).array()

    val bytes = ByteArray(length)
    readBytes(bytes, readerIndex(), length)
    return bytes
}

fun ByteBuf.readAllAvailableBytes(): ByteArray {
    val length = readableBytes()
    if (hasArray()) return array()

    val bytes = ByteArray(length)
    getBytes(readerIndex(), bytes)
    return bytes
}

fun ByteBuf.writeOptionalVarInt(varInt: Optional<Int>) {
    if (varInt.value != null) {
        writeBoolean(true)
        writeVarInt(varInt.value)
    } else {
        writeBoolean(false)
    }
}

fun ByteBuf.writeUUID(uuid: UUID) {
    writeLong(uuid.mostSignificantBits)
    writeLong(uuid.leastSignificantBits)
}

fun ByteBuf.writeOptionalUUID(uuid: Optional<UUID>) {
    if (uuid.value != null) {
        writeBoolean(true)
        writeUUID(uuid.value)
    } else {
        writeBoolean(false)
    }
}

fun ByteBuf.writeNBTCompound(tag: CompoundBinaryTag) {
    val outputStream = ByteArrayOutputStream()
    BinaryTagIO.writer().write(tag, outputStream)
    writeBytes(outputStream.toByteArray())
}

fun ByteBuf.writeChat(component: Component) {
    writeString(GsonComponentSerializer.gson().serialize(component))
}

fun ByteBuf.writeOptionalChat(component: Optional<Component>) {
    if (component.value != null) {
        writeBoolean(true)
        writeChat(component.value)
    } else {
        writeBoolean(false)
    }
}

fun ByteBuf.writeSlot(slot: Slot) {
    writeBoolean(slot.isPresent)
    if (slot.isPresent) {
        writeVarInt(slot.itemId)
        writeByte(slot.itemCount)
        writeNBTCompound(slot.nbt)
    }
}

fun ByteBuf.writeRotation(rotation: Rotation) {
    writeFloat(rotation.x)
    writeFloat(rotation.y)
    writeFloat(rotation.z)
}

fun ByteBuf.writePosition(position: Vector) {
    writeLong(position.toProtocol())
}

fun ByteBuf.writeOptionalPosition(position: Optional<Vector>) {
    if (position.value != null) {
        writeBoolean(true)
        writePosition(position.value)
    } else {
        writeBoolean(false)
    }
}

fun ByteBuf.writeParticle(particle: Particle) {
    writeVarInt(particle.type.id)

    when (particle.type) {
        ParticleType.BLOCK -> writeVarInt((particle as BlockParticle).state)
        ParticleType.DUST -> {
            particle as DustParticle
            writeFloat(particle.red)
            writeFloat(particle.green)
            writeFloat(particle.blue)
            writeFloat(particle.scale)
        }
        ParticleType.FALLING_DUST -> writeVarInt((particle as FallingDustParticle).state)
        ParticleType.ITEM -> writeSlot((particle as ItemParticle).slot)
        else -> Unit
    }
}

fun ByteBuf.writeVillagerData(data: VillagerData) {
    writeVarInt(data.type.id)
    writeVarInt(data.profession.id)
    writeVarInt(data.level)
}

fun ByteBuf.writeAngle(angle: Angle) {
    writeUByte(angle.value)
}

fun ByteBuf.writeKey(key: NamespacedKey) {
    writeString(key.toString())
}

fun ByteBuf.writeDuration(duration: Duration) {
    writeInt(duration.seconds.toInt() * 20)
}

fun Int.varIntSize(): Int {
    for (i in 1 until 5) {
        if ((this and (-1 shl i * 7)) != 0) continue
        return i
    }
    return 5
}