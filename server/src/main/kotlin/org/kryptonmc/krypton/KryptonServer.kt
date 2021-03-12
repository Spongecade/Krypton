package org.kryptonmc.krypton

import com.moandjiezana.toml.Toml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.kryptonmc.krypton.api.Server
import org.kryptonmc.krypton.api.status.StatusInfo
import org.kryptonmc.krypton.command.KryptonCommandManager
import org.kryptonmc.krypton.config.KryptonConfig
import org.kryptonmc.krypton.config.ServerConfig
import org.kryptonmc.krypton.config.StatusConfig
import org.kryptonmc.krypton.config.WorldConfig
import org.kryptonmc.krypton.encryption.Encryption
import org.kryptonmc.krypton.event.KryptonEventManager
import org.kryptonmc.krypton.extension.logger
import org.kryptonmc.krypton.packet.PacketLoader
import org.kryptonmc.krypton.registry.RegistryManager
import org.kryptonmc.krypton.registry.tags.TagManager
import org.kryptonmc.krypton.session.SessionManager
import org.kryptonmc.krypton.api.world.Difficulty
import org.kryptonmc.krypton.api.world.Gamemode
import org.kryptonmc.krypton.entity.entities.KryptonPlayer
import org.kryptonmc.krypton.world.KryptonWorldManager
import org.kryptonmc.krypton.world.scoreboard.KryptonScoreboard
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.util.*
import kotlin.system.exitProcess

class KryptonServer : Server {

    override val info = KryptonServerInfo

    internal val config = loadConfig()

    override val status = KryptonStatusInfo(config.status.maxPlayers, config.status.motd)

    override val isOnline = config.server.onlineMode
    override val isHardcore = config.world.hardcore
    override val difficulty = config.world.difficulty
    override val gamemode = config.world.gamemode

    override lateinit var address: InetSocketAddress

    override val players = mutableSetOf<KryptonPlayer>()

    override fun player(uuid: UUID) = players.firstOrNull { it.uuid == uuid }
    override fun player(name: String) = players.firstOrNull { it.name == name }

    override var scoreboard: KryptonScoreboard? = null

    internal val encryption = Encryption()
    private val nettyProcess = NettyProcess(this)
    internal val random: SecureRandom = SecureRandom()

    val sessionManager = SessionManager(this)

    val registryManager = RegistryManager()
    val tagManager = TagManager()

    override val worldManager = KryptonWorldManager(this, config.world)

    override val commandManager = KryptonCommandManager()
    override val eventManager = KryptonEventManager()

    fun start() {
        PacketLoader.loadAll()
        LOGGER.info("Starting Krypton server on ${config.server.ip}:${config.server.port}...")
        LOGGER.info("Starting Netty...")

        GlobalScope.launch(Dispatchers.IO) {
            nettyProcess.run()
        }

        if (!config.server.onlineMode) {
            LOGGER.warn("-----------------------------------------------------------------------------------")
            LOGGER.warn("SERVER IS IN OFFLINE MODE! THIS SERVER WILL MAKE NO ATTEMPTS TO AUTHENTICATE USERS!")
            LOGGER.warn("While this may allow players without full Minecraft accounts to connect, it also allows hackers to connect with any username they choose! Beware!")
            LOGGER.warn("To get rid of this message, change online_mode to true in config.toml")
            LOGGER.warn("-----------------------------------------------------------------------------------")
        }

        while (true) {
            // keep server alive
        }
    }

    override fun broadcast(message: Component, permission: String?) {
        if (permission != null) players.filter { it.hasPermission(permission) }.forEach { it.sendMessage(message) }
        else sendMessage(message)
    }

    override fun audiences() = players

    private fun loadConfig(): KryptonConfig {
        val configFile = File(Path.of("").toAbsolutePath().toFile(), "config.toml")
        if (!configFile.exists()) {
            val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("config.toml")
                ?: throw IOException("Config file not in classpath! Something has gone horribly wrong!")
            Files.copy(inputStream, configFile.toPath())
        }

        val toml = Toml().read(configFile)
        val serverConfig = toml.getTable("server")
        val statusConfig = toml.getTable("status")
        val worldConfig = toml.getTable("world")

        val gamemode = if (worldConfig.toMap()["gamemode"] is Long) {
            Gamemode.fromId(worldConfig.getLong("gamemode").toInt())
        } else {
            Gamemode.valueOf(worldConfig.getString("gamemode").toUpperCase())
        }
        val difficulty = if (worldConfig.toMap()["difficulty"] is Long) {
            Difficulty.fromId(worldConfig.getLong("difficulty").toInt())
        } else {
            Difficulty.valueOf(worldConfig.getString("difficulty").toUpperCase())
        }

        if (gamemode == null) {
            LOGGER.error("Invalid gamemode! Could not parse value ${worldConfig.getString("gamemode")}!")
            exitProcess(0)
        }
        if (difficulty == null) {
            LOGGER.error("Invalid difficulty! Could not parse value ${worldConfig.getString("difficulty")}!")
            exitProcess(0)
        }

        return KryptonConfig(
            ServerConfig(
                serverConfig.getString("ip"),
                serverConfig.getLong("port").toInt(),
                serverConfig.getBoolean("online_mode"),
                serverConfig.getLong("compression_threshold").toInt()
            ),
            StatusConfig(
                LegacyComponentSerializer.legacyAmpersand().deserialize(statusConfig.getString("motd")),
                statusConfig.getLong("max_players").toInt()
            ),
            WorldConfig(
                worldConfig.getString("name"),
                gamemode,
                difficulty,
                worldConfig.getBoolean("hardcore"),
                worldConfig.getLong("view_distance").toInt()
            )
        )
    }

    object KryptonServerInfo : Server.ServerInfo {

        override val name = "Krypton"
        override val version = "0.10"
        override val minecraftVersion = "1.16.5"
    }

    companion object {

        private val LOGGER = logger<KryptonServer>()
    }
}

data class KryptonStatusInfo(
    override val maxPlayers: Int,
    override val motd: Component
) : StatusInfo