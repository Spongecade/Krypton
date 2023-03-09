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
package org.kryptonmc.krypton.util

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

fun interface TaskChainer {

    fun append(task: DelayedTask)

    fun interface DelayedTask {

        fun submit(executor: Executor): CompletableFuture<*>
    }

    companion object {

        @JvmField
        val LOGGER: Logger = LogManager.getLogger()

        @JvmStatic
        fun immediate(executor: Executor): TaskChainer = TaskChainer { task ->
            task.submit(executor).exceptionally {
                LOGGER.error("Task failed", it)
                null
            }
        }
    }
}