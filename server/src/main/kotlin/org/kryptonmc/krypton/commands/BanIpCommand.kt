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
package org.kryptonmc.krypton.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.kryptonmc.krypton.command.CommandSourceStack
import org.kryptonmc.krypton.command.argument
import org.kryptonmc.krypton.command.permission
import org.kryptonmc.krypton.command.argument.argument
import org.kryptonmc.krypton.command.arguments.CommandExceptions
import org.kryptonmc.krypton.command.literal
import org.kryptonmc.krypton.command.runs
import org.kryptonmc.krypton.locale.Messages
import org.kryptonmc.krypton.server.ban.KryptonIpBan
import org.kryptonmc.krypton.util.asString
import java.util.regex.Pattern

object BanIpCommand {

    private val ERROR_INVALID_IP = CommandExceptions.simple("commands.banip.invalid")
    private val ERROR_ALREADY_BANNED = CommandExceptions.simple("commands.banip.failed")
    @JvmField
    val IP_ADDRESS_PATTERN: Pattern = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")

    private const val TARGET = "target"
    private const val REASON = "reason"
    private const val DEFAULT_REASON = "Banned by operator."

    @JvmStatic
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(literal("ban-ip") {
            permission(KryptonPermission.BAN_IP)
            argument(TARGET, StringArgumentType.string()) {
                runs { banIp(it.source, it.argument(TARGET), DEFAULT_REASON) }
                argument(REASON, StringArgumentType.string()) {
                    runs { banIp(it.source, it.argument(TARGET), it.argument(REASON)) }
                }
            }
        })
    }

    @JvmStatic
    private fun banIp(source: CommandSourceStack, target: String, reason: String) {
        val banManager = source.server.playerManager.banManager
        if (IP_ADDRESS_PATTERN.matcher(target).matches()) {
            // The target is an IP address
            // Check player is not already banned
            if (banManager.isBanned(target)) throw ERROR_ALREADY_BANNED.create()
            banManager.add(KryptonIpBan(target, reason = LegacyComponentSerializer.legacySection().deserialize(reason)))

            // Send success
            source.sendSuccess(Messages.Commands.BAN_IP_SUCCESS.build(target, Component.text(reason)), true)
            return
        }
        val player = source.server.playerManager.getPlayer(target)
        if (player != null) {
            // The target is a player
            val ban = KryptonIpBan(player.address.asString(), reason = LegacyComponentSerializer.legacySection().deserialize(reason))

            // Check player is not already banned
            if (banManager.isBanned(ban.ip)) throw ERROR_ALREADY_BANNED.create()
            banManager.add(ban)

            // Disconnect target
            player.disconnect(Messages.Disconnect.BANNED_IP_MESSAGE.build(ban.reason, ban.expirationDate))
            // Send success
            source.sendSuccess(Messages.Commands.BAN_IP_SUCCESS.build(target, ban.reason), true)
            return
        }
        // The target isn't an IP address or a player
        throw ERROR_INVALID_IP.create()
    }
}
