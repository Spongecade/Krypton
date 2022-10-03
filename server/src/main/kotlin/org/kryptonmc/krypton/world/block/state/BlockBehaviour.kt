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
package org.kryptonmc.krypton.world.block.state

import kotlinx.collections.immutable.ImmutableMap
import net.kyori.adventure.key.Key
import org.kryptonmc.api.block.Block
import org.kryptonmc.api.block.BlockSoundGroup
import org.kryptonmc.api.block.BlockState
import org.kryptonmc.api.block.PushReaction
import org.kryptonmc.api.entity.Hand
import org.kryptonmc.api.item.data.DyeColor
import org.kryptonmc.api.tags.Tag
import org.kryptonmc.api.util.Direction
import org.kryptonmc.krypton.entity.KryptonEntity
import org.kryptonmc.krypton.entity.KryptonEntityType
import org.kryptonmc.krypton.entity.player.KryptonPlayer
import org.kryptonmc.krypton.entity.projectile.KryptonProjectile
import org.kryptonmc.krypton.item.KryptonItemStack
import org.kryptonmc.krypton.item.KryptonItemType
import org.kryptonmc.krypton.item.context.BlockPlaceContext
import org.kryptonmc.krypton.registry.KryptonRegistries
import org.kryptonmc.krypton.shapes.CollisionContext
import org.kryptonmc.krypton.shapes.Shapes
import org.kryptonmc.krypton.shapes.VoxelShape
import org.kryptonmc.krypton.state.KryptonState
import org.kryptonmc.krypton.state.StateDelegate
import org.kryptonmc.krypton.state.property.KryptonProperty
import org.kryptonmc.krypton.util.BlockHitResult
import org.kryptonmc.krypton.util.InteractionResult
import org.kryptonmc.krypton.util.Maths
import org.kryptonmc.krypton.util.math.Mirror
import org.kryptonmc.krypton.util.math.Rotation
import org.kryptonmc.krypton.world.BlockAccessor
import org.kryptonmc.krypton.world.KryptonWorld
import org.kryptonmc.krypton.world.WorldAccessor
import org.kryptonmc.krypton.world.block.BlockSoundGroups
import org.kryptonmc.krypton.world.block.KryptonBlock
import org.kryptonmc.krypton.world.block.RenderShape
import org.kryptonmc.krypton.world.block.SupportType
import org.kryptonmc.krypton.world.block.state.BlockBehaviour.StateArgumentPredicate
import org.kryptonmc.krypton.world.block.state.BlockBehaviour.StatePredicate
import org.kryptonmc.krypton.world.fluid.KryptonFluid
import org.kryptonmc.krypton.world.fluid.KryptonFluidState
import org.kryptonmc.krypton.world.fluid.KryptonFluids
import org.kryptonmc.krypton.world.material.Material
import org.kryptonmc.krypton.world.material.MaterialColor
import org.kryptonmc.serialization.MapCodec
import org.spongepowered.math.vector.Vector3d
import org.spongepowered.math.vector.Vector3i
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.ToIntFunction
import kotlin.math.max

abstract class BlockBehaviour(protected val properties: Properties) : Block {

    protected val material: Material = properties.material
    protected val hasCollision: Boolean = properties.hasCollision
    override val explosionResistance: Double = properties.explosionResistance.toDouble()
    protected val randomlyTicks = properties.randomlyTicks
    final override val soundGroup: BlockSoundGroup = properties.soundGroup
    final override val friction: Double = properties.friction.toDouble()
    val speedFactor: Float = properties.speedFactor
    val jumpFactor: Float = properties.jumpFactor
    protected val hasDynamicShape: Boolean = properties.hasDynamicShape
    protected var drops: Key? = null

    open val maximumHorizontalOffset: Float
        get() = 0.25F
    open val maximumVerticalOffset: Float
        get() = 0.2F
    final override val hasGravity: Boolean
        get() = false // FIXME: When we create all the KryptonBlock implementations, check if is instance of FallingBlock
    final override val hasBlockEntity: Boolean
        get() = false // FIXME: When we create all the KryptonBlock implementations, check if is instance of EntityBlock

    val lootTable: Key
        get() {
            if (drops == null) {
                val registryKey = requireNotNull(KryptonRegistries.BLOCK.get(asBlock())) { "Could not find registry key for block ${asBlock()}!" }
                drops = Key.key(registryKey.namespace(), "blocks/${registryKey.value()}")
            }
            return drops!!
        }
    val defaultMaterialColor: MaterialColor
        get() = properties.materialColor.apply(asBlock().defaultState)
    val defaultDestroyTime: Float
        get() = properties.destroyTime

    // ==============================
    // Block interaction
    // ==============================

    open fun onPlace(state: KryptonBlockState, world: KryptonWorld, x: Int, y: Int, z: Int, old: KryptonBlockState, isMoving: Boolean) {
        // Do nothing by default
    }

    open fun onRemove(state: KryptonBlockState, world: KryptonWorld, x: Int, y: Int, z: Int, new: KryptonBlockState, isMoving: Boolean) {
        if (state.hasBlockEntity && !state.eq(new.block)) {
            // TODO: Remove block entity from world
        }
    }

    open fun getDestroyProgress(state: KryptonBlockState, player: KryptonPlayer, world: BlockAccessor, x: Int, y: Int, z: Int): Float {
        val speed = state.getDestroySpeed(world, x, y, z)
        if (speed == -1F) return 0F
        val correctToolBonus = if (player.hasCorrectTool(state)) 30 else 100
        return player.getDestroySpeed(state) / speed / correctToolBonus
    }

    open fun canBeReplaced(state: KryptonBlockState, context: BlockPlaceContext): Boolean =
        material.replaceable && (context.item.isEmpty() || !context.item.eq(asItem()))

    open fun canBeReplaced(state: KryptonBlockState, fluid: KryptonFluid): Boolean = material.replaceable || !material.solid

    open fun spawnAfterBreak(state: KryptonBlockState, world: KryptonWorld, x: Int, y: Int, z: Int, item: KryptonItemStack, dropItems: Boolean) {
        // Do nothing by default
    }

    open fun attack(state: KryptonBlockState, world: KryptonWorld, x: Int, y: Int, z: Int, player: KryptonPlayer) {
        // Do nothing by default
    }

    open fun use(
        state: KryptonBlockState,
        world: KryptonWorld,
        position: Vector3i,
        player: KryptonPlayer,
        hand: Hand,
        hit: BlockHitResult
    ): InteractionResult = InteractionResult.PASS

    open fun triggerEvent(state: KryptonBlockState, world: KryptonWorld, x: Int, y: Int, z: Int, id: Int, parameter: Int): Boolean = false

    open fun useShapeForLightOcclusion(state: KryptonBlockState): Boolean = false

    open fun getLightBlock(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int): Int {
        if (state.isSolidRender(world, x, y, z)) return world.maximumLightLevel
        if (state.propagatesSkylightDown(world, x, y, z)) return 0
        return 1
    }

    // ==============================
    // Shapes
    // ==============================

    open fun getShape(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int, context: CollisionContext): VoxelShape = Shapes.block()

    open fun getOcclusionShape(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int): VoxelShape = state.getShape(world, x, y, z)

    open fun getCollisionShape(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int, context: CollisionContext): VoxelShape =
        if (hasCollision) state.getShape(world, x, y, z) else Shapes.empty()

    open fun getVisualShape(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int, context: CollisionContext): VoxelShape =
        getCollisionShape(state, world, x, y, z, context)

    open fun getInteractionShape(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int): VoxelShape = Shapes.empty()

    open fun getBlockSupportShape(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int): VoxelShape =
        getCollisionShape(state, world, x, y, z, CollisionContext.empty())

    open fun isCollisionShapeFullBlock(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int): Boolean =
        KryptonBlock.isShapeFullBlock(state.getCollisionShape(world, x, y, z))

    open fun isOcclusionShapeFullBlock(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int): Boolean =
        KryptonBlock.isShapeFullBlock(state.getOcclusionShape(world, x, y, z))

    open fun updateIndirectNeighbourShapes(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int, flags: Int, recursionLeft: Int) {
        // Do nothing by default
    }

    open fun updateShape(
        state: KryptonBlockState,
        direction: Direction,
        neighbour: KryptonBlockState,
        world: BlockAccessor,
        currentX: Int,
        currentY: Int,
        currentZ: Int,
        neighbourX: Int,
        neighbourY: Int,
        neighbourZ: Int
    ): KryptonBlockState = state

    // ==============================
    // Redstone stuff
    // ==============================

    open fun isSignalSource(state: KryptonBlockState): Boolean = false

    open fun hasAnalogOutputSignal(state: KryptonBlockState): Boolean = false

    open fun getAnalogOutputSignal(state: KryptonBlockState, world: KryptonWorld, x: Int, y: Int, z: Int): Int = 0

    open fun getSignal(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int, direction: Direction): Int = 0

    open fun getDirectSignal(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int, direction: Direction): Int = 0

    // ==============================
    // Miscellaneous
    // ==============================

    open fun getRenderShape(state: KryptonBlockState): RenderShape = RenderShape.MODEL

    open fun getPushReaction(state: KryptonBlockState): PushReaction = material.pushReaction

    open fun getFluidState(state: KryptonBlockState): KryptonFluidState = KryptonFluids.EMPTY.defaultState

    open fun canSurvive(state: KryptonBlockState, world: WorldAccessor, x: Int, y: Int, z: Int): Boolean = true

    open fun entityInside(state: KryptonBlockState, world: KryptonWorld, x: Int, y: Int, z: Int, entity: KryptonEntity) {
        // Do nothing by default
    }

    open fun onProjectileHit(world: KryptonWorld, state: KryptonBlockState, hit: BlockHitResult, projectile: KryptonProjectile) {
        // Do nothing by default
    }

    open fun skipRendering(state: KryptonBlockState, adjacent: KryptonBlockState, direction: Direction): Boolean = false

    open fun rotate(state: KryptonBlockState, rotation: Rotation): KryptonBlockState = state

    open fun mirror(state: KryptonBlockState, mirror: Mirror): KryptonBlockState = state

    abstract override fun asItem(): KryptonItemType

    abstract override fun asBlock(): KryptonBlock

    enum class OffsetType {

        NONE,
        XZ,
        XYZ
    }

    abstract class BlockStateBase(
        owner: KryptonBlock,
        values: ImmutableMap<KryptonProperty<*>, Comparable<*>>,
        propertiesCodec: MapCodec<KryptonBlockState>
    ) : KryptonState<KryptonBlock, KryptonBlockState>(owner, values, propertiesCodec), BlockState, StateDelegate<BlockState, KryptonBlockState> {

        val lightEmission: Int = owner.properties.lightEmission.applyAsInt(asState())
        val useShapeForLightOcclusion: Boolean = owner.useShapeForLightOcclusion(asState())
        val material: Material = owner.properties.material
        private val materialColor = owner.properties.materialColor.apply(asState())
        private val destroySpeed = owner.properties.destroyTime
        val requiresCorrectTool: Boolean = owner.properties.requiresCorrectTool
        val canOcclude: Boolean = owner.properties.canOcclude
        private val isRedstoneConductor = owner.properties.isRedstoneConductor
        private val isSuffocating = owner.properties.isSuffocating
        private val hasPostProcess = owner.properties.hasPostProcess
        val offsetType: OffsetType = owner.properties.offsetType.apply(asState())
        protected var cache: Cache? = null

        final override val isAir: Boolean
            get() = block.properties.isAir
        final override val hardness: Double
            get() = destroySpeed.toDouble()
        final override val isOpaque: Boolean
            get() = canOcclude
        final override val isSolid: Boolean
            get() = material.solid
        final override val blocksMotion: Boolean
            get() = material.blocksMotion
        final override val isFlammable: Boolean
            get() = material.flammable
        final override val isLiquid: Boolean
            get() = material.liquid
        final override val isReplaceable: Boolean
            get() = material.replaceable
        final override val pushReaction: PushReaction
            get() = block.getPushReaction(asState())

        final override val block: KryptonBlock
            get() = owner
        final override val state: KryptonBlockState
            get() = asState()
        val hasBlockEntity: Boolean
            get() = false // FIXME: When we create all the KryptonBlock implementations, check if is instance of EntityBlock
        val hasLargeCollisionShape: Boolean
            get() = cache == null || cache!!.largeCollisionShape
        val renderShape: RenderShape
            get() = block.getRenderShape(asState())
        val isSignalSource: Boolean
            get() = block.isSignalSource(asState())
        val hasAnalogOutputSignal: Boolean
            get() = block.hasAnalogOutputSignal(asState())
        val soundGroup: BlockSoundGroup
            get() = block.properties.soundGroup
        val randomlyTicks: Boolean
            get() = block.randomlyTicks(asState())

        fun initCache() {
            if (!block.hasDynamicShape) cache = Cache(asState())
        }

        fun isValidSpawn(world: BlockAccessor, x: Int, y: Int, z: Int, type: KryptonEntityType<*>): Boolean =
            block.properties.isValidSpawn.test(asState(), world, x, y, z, type)

        fun propagatesSkylightDown(world: BlockAccessor, x: Int, y: Int, z: Int): Boolean =
            cache?.propagatesSkylightDown ?: block.propagatesSkylightDown(asState(), world, x, y, z)

        fun getLightBlock(world: BlockAccessor, x: Int, y: Int, z: Int): Int = cache?.lightBlock ?: block.getLightBlock(asState(), world, x, y, z)

        fun getMapColor(world: BlockAccessor, x: Int, y: Int, z: Int): MaterialColor = materialColor

        fun rotate(rotation: Rotation): KryptonBlockState = block.rotate(asState(), rotation)

        fun mirror(mirror: Mirror): KryptonBlockState = block.mirror(asState(), mirror)

        fun isRedstoneConductor(world: BlockAccessor, x: Int, y: Int, z: Int): Boolean = isRedstoneConductor.test(asState(), world, x, y, z)

        fun getSignal(world: BlockAccessor, x: Int, y: Int, z: Int, direction: Direction): Int =
            block.getSignal(asState(), world, x, y, z, direction)

        fun getDirectSignal(world: BlockAccessor, x: Int, y: Int, z: Int, direction: Direction): Int =
            block.getDirectSignal(asState(), world, x, y, z, direction)

        fun getAnalogOutputSignal(world: KryptonWorld, x: Int, y: Int, z: Int): Int = block.getAnalogOutputSignal(asState(), world, x, y, z)

        fun getDestroySpeed(world: BlockAccessor, x: Int, y: Int, z: Int): Float = destroySpeed

        fun getDestroyProgress(player: KryptonPlayer, world: BlockAccessor, x: Int, y: Int, z: Int): Float =
            block.getDestroyProgress(asState(), player, world, x, y, z)

        fun isSolidRender(world: BlockAccessor, x: Int, y: Int, z: Int): Boolean {
            if (cache != null) return cache!!.solidRender
            val state = asState()
            return if (state.canOcclude) KryptonBlock.isShapeFullBlock(state.getOcclusionShape(world, x, y, z)) else false
        }

        fun skipRendering(state: KryptonBlockState, face: Direction): Boolean = block.skipRendering(asState(), state, face)

        fun getShape(world: BlockAccessor, x: Int, y: Int, z: Int): VoxelShape = getShape(world, x, y, z, CollisionContext.empty())

        fun getShape(world: BlockAccessor, x: Int, y: Int, z: Int, context: CollisionContext): VoxelShape =
            block.getShape(asState(), world, x, y, z, context)

        fun getFaceOcclusionShape(world: BlockAccessor, x: Int, y: Int, z: Int, face: Direction): VoxelShape =
            cache?.occlusionShapes?.get(face.ordinal) ?: Shapes.faceShape(getOcclusionShape(world, x, y, z), face)

        fun getOcclusionShape(world: BlockAccessor, x: Int, y: Int, z: Int): VoxelShape = block.getOcclusionShape(asState(), world, x, y, z)

        fun getCollisionShape(world: BlockAccessor, x: Int, y: Int, z: Int): VoxelShape =
            cache?.collisionShape ?: getCollisionShape(world, x, y, z, CollisionContext.empty())

        fun getCollisionShape(world: BlockAccessor, x: Int, y: Int, z: Int, context: CollisionContext): VoxelShape =
            block.getCollisionShape(asState(), world, x, y, z, context)

        fun getBlockSupportShape(world: BlockAccessor, x: Int, y: Int, z: Int): VoxelShape = block.getBlockSupportShape(asState(), world, x, y, z)

        fun getVisualShape(world: BlockAccessor, x: Int, y: Int, z: Int, context: CollisionContext): VoxelShape =
            block.getVisualShape(asState(), world, x, y, z, context)

        fun getInteractionShape(world: BlockAccessor, x: Int, y: Int, z: Int): VoxelShape = block.getInteractionShape(asState(), world, x, y, z)

        fun entityCanStandOn(world: BlockAccessor, x: Int, y: Int, z: Int, entity: KryptonEntity): Boolean =
            entityCanStandOnFace(world, x, y, z, entity, Direction.UP)

        fun entityCanStandOnFace(world: BlockAccessor, x: Int, y: Int, z: Int, entity: KryptonEntity, face: Direction): Boolean =
            KryptonBlock.isFaceFull(getCollisionShape(world, x, y, z, CollisionContext.of(entity)), face)

        fun getOffset(world: BlockAccessor, x: Int, y: Int, z: Int): Vector3d {
            if (offsetType == OffsetType.NONE) return Vector3d.ZERO
            val seed = Maths.getSeed(x, 0, z)
            val maxHorizontalOffset = block.maximumHorizontalOffset.toDouble()
            val offsetX = Maths.clamp((((seed and 15L) / 15F) - 0.5) * 0.5, -maxHorizontalOffset, maxHorizontalOffset)
            val offsetY = if (offsetType == OffsetType.XYZ) ((((seed shr 4) and 15L) / 15F) - 1.0) * block.maximumVerticalOffset else 0.0
            val offsetZ = Maths.clamp(((((seed shr 8) and 15) / 15F) - 0.5) * 0.5, -maxHorizontalOffset, maxHorizontalOffset)
            return Vector3d(offsetX, offsetY, offsetZ)
        }

        fun triggerEvent(world: KryptonWorld, x: Int, y: Int, z: Int, id: Int, parameter: Int): Boolean =
            block.triggerEvent(asState(), world, x, y, z, id, parameter)

        fun updateIndirectNeighbourShapes(world: WorldAccessor, x: Int, y: Int, z: Int, flags: Int) {
            updateIndirectNeighbourShapes(world, x, y, z, flags, 512)
        }

        fun updateIndirectNeighbourShapes(world: WorldAccessor, x: Int, y: Int, z: Int, flags: Int, recursionLeft: Int) {
            block.updateIndirectNeighbourShapes(asState(), world, x, y, z, flags, recursionLeft)
        }

        fun onPlace(world: KryptonWorld, x: Int, y: Int, z: Int, old: KryptonBlockState, isMoving: Boolean) {
            block.onPlace(asState(), world, x, y, z, old, isMoving)
        }

        fun onRemove(world: KryptonWorld, x: Int, y: Int, z: Int, new: KryptonBlockState, isMoving: Boolean) {
            block.onRemove(asState(), world, x, y, z, new, isMoving)
        }

        fun entityInside(world: KryptonWorld, x: Int, y: Int, z: Int, entity: KryptonEntity) {
            block.entityInside(asState(), world, x, y, z, entity)
        }

        fun spawnAfterBreak(world: KryptonWorld, x: Int, y: Int, z: Int, item: KryptonItemStack, dropItems: Boolean) {
            block.spawnAfterBreak(asState(), world, x, y, z, item, dropItems)
        }

        fun use(world: KryptonWorld, player: KryptonPlayer, hand: Hand, hit: BlockHitResult): InteractionResult =
            block.use(asState(), world, hit.position, player, hand, hit)

        fun attack(world: KryptonWorld, x: Int, y: Int, z: Int, player: KryptonPlayer) {
            block.attack(asState(), world, x, y, z, player)
        }

        fun isSuffocating(world: BlockAccessor, x: Int, y: Int, z: Int): Boolean = isSuffocating.test(asState(), world, x, y, z)

        fun updateShape(
            direction: Direction,
            neighbour: KryptonBlockState,
            world: WorldAccessor,
            currentX: Int,
            currentY: Int,
            currentZ: Int,
            neighbourX: Int,
            neighbourY: Int,
            neighbourZ: Int
        ): KryptonBlockState =
            block.updateShape(asState(), direction, neighbour, world, currentX, currentY, currentZ, neighbourX, neighbourY, neighbourZ)

        fun canBeReplaced(context: BlockPlaceContext): Boolean = block.canBeReplaced(asState(), context)

        fun canBeReplaced(fluid: KryptonFluid): Boolean = block.canBeReplaced(asState(), fluid)

        fun onProjectileHit(world: KryptonWorld, state: KryptonBlockState, hit: BlockHitResult, projectile: KryptonProjectile) {
            block.onProjectileHit(world, state, hit, projectile)
        }

        fun isFaceSturdy(world: BlockAccessor, x: Int, y: Int, z: Int, face: Direction): Boolean =
            isFaceSturdy(world, x, y, z, face, SupportType.FULL)

        fun isFaceSturdy(world: BlockAccessor, x: Int, y: Int, z: Int, face: Direction, type: SupportType): Boolean =
            cache?.isFaceSturdy(face, type) ?: type.isSupporting(asState(), world, x, y, z, face)

        fun isCollisionShapeFullBlock(world: BlockAccessor, x: Int, y: Int, z: Int): Boolean =
            cache?.isCollisionShapeFullBlock ?: block.isCollisionShapeFullBlock(asState(), world, x, y, z)

        fun eq(tag: Tag<Block>): Boolean = tag.contains(block)

        fun eq(tag: Tag<Block>, predicate: Predicate<BlockStateBase>): Boolean = eq(tag) && predicate.test(this)

        fun eq(block: Block): Boolean = this.block === block

        final override fun asFluid(): KryptonFluidState = block.getFluidState(asState())

        protected abstract fun asState(): KryptonBlockState

        protected class Cache(state: KryptonBlockState) {

            val solidRender: Boolean = state.isSolidRender(BlockAccessor.Empty, 0, 0, 0)
            val propagatesSkylightDown: Boolean = state.block.propagatesSkylightDown(state, BlockAccessor.Empty, 0, 0, 0)
            val lightBlock: Int = state.block.getLightBlock(state, BlockAccessor.Empty, 0, 0, 0)
            val occlusionShapes: Array<VoxelShape>? = createOcclusionShapes(state)
            val collisionShape: VoxelShape = state.block.getCollisionShape(state, BlockAccessor.Empty, 0, 0, 0, CollisionContext.empty())
            val largeCollisionShape: Boolean
            private val faceSturdy = BooleanArray(DIRECTIONS.size * SUPPORT_TYPES.size)
            val isCollisionShapeFullBlock: Boolean

            init {
                if (!collisionShape.isEmpty() && state.offsetType != OffsetType.NONE) {
                    val key = KryptonRegistries.BLOCK.get(state.block)
                    error("$key has a collision shape and an offset type, but is not marked as dynamic in its properties!")
                }
                largeCollisionShape = Direction.Axis.values().any { collisionShape.min(it) < 0.0 || collisionShape.max(it) > 1.0 }
                DIRECTIONS.forEach { direction ->
                    SUPPORT_TYPES.forEach {
                        faceSturdy[getFaceSupportIndex(direction, it)] = it.isSupporting(state, BlockAccessor.Empty, 0, 0, 0, direction)
                    }
                }
                isCollisionShapeFullBlock = KryptonBlock.isShapeFullBlock(state.getCollisionShape(BlockAccessor.Empty, 0, 0, 0))
            }

            fun isFaceSturdy(direction: Direction, type: SupportType): Boolean = faceSturdy[getFaceSupportIndex(direction, type)]

            companion object {

                private val DIRECTIONS = Direction.values()
                private val SUPPORT_TYPES = SupportType.values()

                @JvmStatic
                @Suppress("UNCHECKED_CAST")
                private fun createOcclusionShapes(state: KryptonBlockState): Array<VoxelShape>? {
                    if (!state.canOcclude) return null
                    val result = arrayOfNulls<VoxelShape>(DIRECTIONS.size)
                    val shape = state.block.getOcclusionShape(state, BlockAccessor.Empty, 0, 0, 0)
                    DIRECTIONS.forEach { result[it.ordinal] = Shapes.faceShape(shape, it) }
                    return result as Array<VoxelShape>
                }

                @JvmStatic
                private fun getFaceSupportIndex(direction: Direction, type: SupportType): Int =
                    direction.ordinal * SUPPORT_TYPES.size + type.ordinal
            }
        }
    }

    class Properties private constructor(material: Material, materialColor: Function<BlockState, MaterialColor>) {

        var material: Material = material
            private set
        var materialColor: Function<BlockState, MaterialColor> = materialColor
            private set
        var hasCollision: Boolean = true
            private set
        var soundGroup: BlockSoundGroup = BlockSoundGroups.STONE
            private set
        var lightEmission: ToIntFunction<BlockState> = ToIntFunction { 0 }
            private set
        var explosionResistance: Float = 0F
            private set
        var destroyTime: Float = 0F
            private set
        var requiresCorrectTool: Boolean = false
            private set
        var randomlyTicks: Boolean = false
            private set
        var friction: Float = 0.6F
            private set
        var speedFactor: Float = 1F
            private set
        var jumpFactor: Float = 1F
            private set
        var drops: Key? = null
            private set
        var canOcclude: Boolean = true
            private set
        var isAir: Boolean = false
            private set
        var isValidSpawn: StateArgumentPredicate<KryptonEntityType<*>> = defaultValidSpawn()
            private set
        var isRedstoneConductor: StatePredicate = defaultRedstoneConductor()
            private set
        var isSuffocating: StatePredicate = defaultSuffocating(material)
            private set
        var hasPostProcess: StatePredicate = StatePredicate { _, _, _, _, _ -> false }
            private set
        var hasDynamicShape: Boolean = false
            private set
        var offsetType: Function<KryptonBlockState, OffsetType> = Function { OffsetType.NONE }
            private set

        fun noCollision(): Properties = apply {
            hasCollision = false
            canOcclude = false
        }

        fun noOcclusion(): Properties = apply { canOcclude = false }

        fun friction(value: Float): Properties = apply { friction = value }

        fun speedFactor(value: Float): Properties = apply { speedFactor = value }

        fun jumpFactor(value: Float): Properties = apply { jumpFactor = value }

        fun sounds(group: BlockSoundGroup): Properties = apply { soundGroup = group }

        fun lightLevel(emission: ToIntFunction<BlockState>): Properties = apply { lightEmission = emission }

        fun strength(hardness: Float, resistance: Float): Properties = destroyTime(hardness).explosionResistance(resistance)

        fun instabreak(): Properties = strength(0F)

        fun strength(strength: Float): Properties = strength(strength, strength)

        fun randomlyTicks(): Properties = apply { randomlyTicks = true }

        fun dynamicShape(): Properties = apply { hasDynamicShape = true }

        fun noLootTable(): Properties = apply { drops = EMPTY_LOOT_TABLE_KEY }

        fun dropsLike(block: KryptonBlock): Properties = apply { drops = block.lootTable }

        fun air(): Properties = apply { isAir = true }

        fun isValidSpawn(predicate: StateArgumentPredicate<KryptonEntityType<*>>): Properties = apply { isValidSpawn = predicate }

        fun isRedstoneConductor(predicate: StatePredicate): Properties = apply { isRedstoneConductor = predicate }

        fun isSuffocating(predicate: StatePredicate): Properties = apply { isSuffocating = predicate }

        fun hasPostProcess(predicate: StatePredicate): Properties = apply { hasPostProcess = predicate }

        fun requiresCorrectTool(): Properties = apply { requiresCorrectTool = true }

        fun color(color: MaterialColor): Properties = apply { materialColor = Function { color } }

        fun destroyTime(value: Float): Properties = apply { destroyTime = value }

        fun explosionResistance(resistance: Float): Properties = apply { explosionResistance = max(0F, resistance) }

        fun offsetType(type: OffsetType): Properties = offsetType { type }

        fun offsetType(getter: Function<KryptonBlockState, OffsetType>): Properties = apply { offsetType = getter }

        companion object {

            private val EMPTY_LOOT_TABLE_KEY = Key.key("empty")

            @JvmStatic
            fun of(material: Material): Properties = of(material, material.color)

            @JvmStatic
            fun of(material: Material, color: DyeColor): Properties = of(material, MaterialColor.fromDyeColor(color))

            @JvmStatic
            fun of(material: Material, color: MaterialColor): Properties = of(material) { color }

            @JvmStatic
            fun of(material: Material, color: Function<BlockState, MaterialColor>): Properties = Properties(material, color)

            @JvmStatic
            fun from(block: BlockBehaviour): Properties {
                val properties = Properties(block.material, block.properties.materialColor)
                properties.material = block.properties.material
                properties.destroyTime = block.properties.destroyTime
                properties.explosionResistance = block.properties.explosionResistance
                properties.hasCollision = block.properties.hasCollision
                properties.randomlyTicks = block.properties.randomlyTicks
                properties.lightEmission = block.properties.lightEmission
                properties.materialColor = block.properties.materialColor
                properties.soundGroup = block.properties.soundGroup
                properties.friction = block.properties.friction
                properties.speedFactor = block.properties.speedFactor
                properties.hasDynamicShape = block.properties.hasDynamicShape
                properties.canOcclude = block.properties.canOcclude
                properties.isAir = block.properties.isAir
                properties.requiresCorrectTool = block.properties.requiresCorrectTool
                properties.offsetType = block.properties.offsetType
                return properties
            }

            @JvmStatic
            private fun defaultValidSpawn(): StateArgumentPredicate<KryptonEntityType<*>> =
                StateArgumentPredicate { state, world, x, y, z, _ -> state.isFaceSturdy(world, x, y, z, Direction.UP) && state.lightEmission < 14 }

            @JvmStatic
            private fun defaultRedstoneConductor(): StatePredicate =
                StatePredicate { state, world, x, y, z -> state.material.solidBlocking && state.isCollisionShapeFullBlock(world, x, y, z) }

            @JvmStatic
            private fun defaultSuffocating(material: Material): StatePredicate =
                StatePredicate { state, world, x, y, z -> material.blocksMotion && state.isCollisionShapeFullBlock(world, x, y, z) }
        }
    }

    fun interface StateArgumentPredicate<T> {

        fun test(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int, type: T): Boolean
    }

    fun interface StatePredicate {

        fun test(state: KryptonBlockState, world: BlockAccessor, x: Int, y: Int, z: Int): Boolean
    }

    companion object {

        private val UPDATE_SHAPE_ORDER = arrayOf(Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP)
    }
}