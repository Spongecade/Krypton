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
package org.kryptonmc.krypton.command.registrar

import com.mojang.brigadier.tree.LiteralCommandNode
import com.mojang.brigadier.tree.RootCommandNode
import org.kryptonmc.api.command.Command
import org.kryptonmc.api.command.meta.CommandMeta
import org.kryptonmc.api.command.CommandRegistrar
import org.kryptonmc.api.command.Sender
import org.kryptonmc.krypton.command.copy
import java.util.concurrent.locks.Lock

abstract class KryptonCommandRegistrar<C : Command, M : CommandMeta>(private val lock: Lock) : CommandRegistrar<C, M> {

    protected fun register(root: RootCommandNode<Sender>, node: LiteralCommandNode<Sender>) {
        lock.lock()
        try {
            root.removeChildByName(node.name)
            root.addChild(node)
        } finally {
            lock.unlock()
        }
    }

    // This shallow copies the node to get around https://github.com/Mojang/brigadier/issues/46
    protected fun register(root: RootCommandNode<Sender>, node: LiteralCommandNode<Sender>, alias: String) = register(root, node.copy(alias))
}