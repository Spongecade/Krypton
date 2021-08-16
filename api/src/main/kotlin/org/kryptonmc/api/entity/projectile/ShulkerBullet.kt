/*
 * This file is part of the Krypton API, licensed under the MIT license.
 *
 * Copyright (C) 2021 KryptonMC and the contributors to the Krypton project.
 *
 * This project is licensed under the terms of the MIT license.
 * For more details, please reference the LICENSE file in the api top-level directory.
 */
package org.kryptonmc.api.entity.projectile

import org.kryptonmc.api.entity.Entity
import org.kryptonmc.api.space.Direction

/**
 * A bullet fired from a [Shulker].
 */
interface ShulkerBullet : Projectile {

    /**
     * How many steps the bullet will take to attack the target.
     *
     * The higher this value is, the further out of the way the bullet travels to reach
     * the target.
     * If this value is 0, the bullet makes no attempt to attack the target, and instead
     * uses
     */
    val steps: Int

    /**
     * The target of this bullet, or null if this bullet does not have a target established.
     */
    val target: Entity?

    /**
     * The current direction that this bullet is moving, or null if this bullet is not
     * moving.
     */
    val movingDirection: Direction?

    /**
     * The X offset of the target from the location of this bullet.
     */
    val targetDeltaX: Double

    /**
     * The Y offset of the target from the location of this bullet.
     */
    val targetDeltaY: Double

    /**
     * The Z offset of the target from the location of this bullet.
     */
    val targetDeltaZ: Double
}
