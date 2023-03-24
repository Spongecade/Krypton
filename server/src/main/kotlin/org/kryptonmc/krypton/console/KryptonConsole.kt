/*
 * This file is part of the Krypton project, licensed under the Apache License v2.0
 *
 * Copyright (C) 2021-2023 KryptonMC and the contributors of the Krypton project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kryptonmc.krypton.console

import net.kyori.adventure.identity.Identity
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.pointer.Pointers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.util.TriState
import net.minecrell.terminalconsole.SimpleTerminalConsole
import org.apache.logging.log4j.LogManager
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.kryptonmc.api.command.ConsoleSender
import org.kryptonmc.api.permission.PermissionFunction
import org.kryptonmc.api.util.Position
import org.kryptonmc.krypton.KryptonServer
import org.kryptonmc.krypton.command.CommandSourceStack
import org.kryptonmc.krypton.command.KryptonSender
import org.kryptonmc.krypton.event.command.KryptonCommandExecuteEvent
import org.kryptonmc.krypton.event.server.KryptonSetupPermissionsEvent
import org.kryptonmc.krypton.locale.MinecraftTranslationManager
import org.kryptonmc.krypton.network.chat.RichChatType

class KryptonConsole(override val server: KryptonServer) : SimpleTerminalConsole(), KryptonSender, ConsoleSender {

    // The permission function defaults to ALWAYS_TRUE because we are god and have all permissions by default
    private var permissionFunction = DEFAULT_PERMISSION_FUNCTION
    private var cachedPointers: Pointers? = null

    override val name: String
        get() = NAME

    fun setupPermissions() {
        val event = server.eventNode.fire(KryptonSetupPermissionsEvent(this, DEFAULT_PERMISSION_FUNCTION))
        permissionFunction = event.result?.function ?: event.defaultFunction
    }

    fun run() {
        val thread = Thread({ start() }, "Console Handler").apply {
            setUncaughtExceptionHandler { _, exception -> LOGGER.error("Caught previously unhandled exception!", exception) }
            isDaemon = true
        }
        thread.start()
    }

    override fun sendSystemMessage(message: Component) {
        LOGGER.info(LegacyComponentSerializer.legacySection().serialize(MinecraftTranslationManager.render(message)))
    }

    fun logChatMessage(message: Component, type: RichChatType.Bound, prefix: String?) {
        if (!server.config.advanced.logPlayerChatMessages) return

        val messageText = LegacyComponentSerializer.legacySection().serialize(MinecraftTranslationManager.render(type.decorate(message)))
        if (prefix != null) LOGGER.info("[$prefix] $messageText") else LOGGER.info(messageText)
    }

    override fun getPermissionValue(permission: String): TriState = permissionFunction.getPermissionValue(permission)

    override fun isRunning(): Boolean = server.isRunning()

    override fun runCommand(command: String) {
        val event = server.eventNode.fire(KryptonCommandExecuteEvent(this, command))
        if (event.isAllowed()) server.commandManager.dispatch(createCommandSourceStack(), event.result?.command ?: command)
    }

    override fun shutdown() {
        server.stop()
    }

    override fun buildReader(builder: LineReaderBuilder): LineReader = super.buildReader(
        builder.appName("Krypton")
            .completer(BrigadierCompleter(server.commandManager) { createCommandSourceStack() })
            .highlighter(BrigadierHighlighter(server.commandManager) { createCommandSourceStack() })
            .option(LineReader.Option.COMPLETE_IN_WORD, true)
    )

    override fun pointers(): Pointers {
        if (cachedPointers == null) {
            cachedPointers = Pointers.builder()
                .withStatic(Identity.NAME, NAME)
                .withStatic(Identity.DISPLAY_NAME, DISPLAY_NAME)
                .withStatic(PermissionChecker.POINTER, PermissionChecker { getPermissionValue(it) })
                .build()
        }
        return cachedPointers!!
    }

    override fun acceptsSuccess(): Boolean = true

    override fun acceptsFailure(): Boolean = true

    override fun shouldInformAdmins(): Boolean = server.config.server.broadcastConsoleToAdmins

    override fun createCommandSourceStack(): CommandSourceStack =
        CommandSourceStack(this, Position.ZERO, server.worldManager.default, NAME, DISPLAY_NAME, server, null)

    companion object {

        private const val NAME = "CONSOLE"
        private val DISPLAY_NAME = Component.text(NAME)

        private val DEFAULT_PERMISSION_FUNCTION = PermissionFunction.ALWAYS_TRUE
        private val LOGGER = LogManager.getLogger("CONSOLE")
    }
}
