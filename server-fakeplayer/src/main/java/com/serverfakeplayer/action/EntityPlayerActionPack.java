package com.serverfakeplayer.action;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Simplified Carpet EntityPlayerActionPack for common actions.
 */
public final class EntityPlayerActionPack {

    private final ServerPlayer player;
    private final Map<ActionType, Action> actions = new EnumMap<>(ActionType.class);

    private BlockPos currentBlock;
    private int blockHitDelay;
    private float curBlockDamageMP;
    private boolean sneaking;
    private boolean sprinting;
    private float forward;
    private float strafing;
    private int itemUseCooldown;

    public EntityPlayerActionPack(ServerPlayer player) {
        this.player = player;
        stopAll();
    }

    public EntityPlayerActionPack start(ActionType type, Action action) {
        Action previous = actions.remove(type);
        if (previous != null) {
            type.stop(player, previous);
        }
        if (action != null) {
            actions.put(type, action);
        }
        return this;
    }

    public EntityPlayerActionPack setSneaking(boolean doSneak) {
        sneaking = doSneak;
        player.setShiftKeyDown(doSneak);
        if (sprinting && sneaking) {
            setSprinting(false);
        }
        return this;
    }

    public EntityPlayerActionPack setSprinting(boolean doSprint) {
        sprinting = doSprint;
        player.setSprinting(doSprint);
        if (sneaking && sprinting) {
            setSneaking(false);
        }
        return this;
    }

    public EntityPlayerActionPack setForward(float value) {
        forward = value;
        return this;
    }

    public EntityPlayerActionPack setStrafing(float value) {
        strafing = value;
        return this;
    }

    public EntityPlayerActionPack look(float yaw, float pitch) {
        player.setYRot(yaw % 360);
        player.setXRot(Mth.clamp(pitch, -90, 90));
        player.setYHeadRot(player.getYRot());
        return this;
    }

    public EntityPlayerActionPack look(Direction direction) {
        return switch (direction) {
            case NORTH -> look(180, 0);
            case SOUTH -> look(0, 0);
            case EAST -> look(-90, 0);
            case WEST -> look(90, 0);
            case UP -> look(player.getYRot(), -90);
            case DOWN -> look(player.getYRot(), 90);
        };
    }

    public EntityPlayerActionPack lookAt(Vec3 position) {
        player.lookAt(EntityAnchorArgument.Anchor.EYES, position);
        return this;
    }

    public EntityPlayerActionPack stopMovement() {
        setSneaking(false);
        setSprinting(false);
        forward = 0.0F;
        strafing = 0.0F;
        return this;
    }

    public EntityPlayerActionPack stopAll() {
        for (Map.Entry<ActionType, Action> entry : actions.entrySet()) {
            entry.getKey().stop(player, entry.getValue());
        }
        actions.clear();
        return stopMovement();
    }

    public void onUpdate() {
        actions.values().removeIf(action -> action.done);
        for (Map.Entry<ActionType, Action> entry : Map.copyOf(actions).entrySet()) {
            entry.getValue().tick(this, entry.getKey());
        }
        float vel = sneaking ? 0.3F : 1.0F;
        player.zza = forward * vel;
        player.xxa = strafing * vel;
    }

    static HitResult getTarget(ServerPlayer player) {
        double reach = player.gameMode.isCreative() ? 5.0 : 4.5;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(reach));

        BlockHitResult blockHit = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
        ));

        double blockDist = blockHit.getType() == HitResult.Type.MISS
                ? reach * reach
                : eye.distanceToSqr(blockHit.getLocation());

        AABB box = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player,
                eye,
                end,
                box,
                entity -> !entity.isSpectator() && entity.isPickable() && entity != player,
                blockDist
        );
        if (entityHit != null) {
            return entityHit;
        }
        return blockHit;
    }

    public enum ActionType {
        USE {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                EntityPlayerActionPack ap = pack(player);
                if (ap.itemUseCooldown > 0) {
                    ap.itemUseCooldown--;
                    return true;
                }
                if (player.isUsingItem()) {
                    return true;
                }
                HitResult hit = getTarget(player);
                for (InteractionHand hand : InteractionHand.values()) {
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        BlockHitResult blockHit = (BlockHitResult) hit;
                        BlockPos pos = blockHit.getBlockPos();
                        ServerLevel world = player.level();
                        if (world.mayInteract(player, pos)) {
                            InteractionResult result = player.gameMode.useItemOn(
                                    player, world, player.getItemInHand(hand), hand, blockHit
                            );
                            if (result.consumesAction()) {
                                if (result instanceof InteractionResult.Success success
                                        && success.swingSource() == InteractionResult.SwingSource.SERVER) {
                                    player.swing(hand);
                                }
                                ap.itemUseCooldown = 3;
                                return true;
                            }
                        }
                    } else if (hit.getType() == HitResult.Type.ENTITY) {
                        EntityHitResult entityHit = (EntityHitResult) hit;
                        Entity entity = entityHit.getEntity();
                        Vec3 relative = entityHit.getLocation().subtract(entity.getX(), entity.getY(), entity.getZ());
                        if (player.interactOn(entity, hand, relative).consumesAction()) {
                            ap.itemUseCooldown = 3;
                            return true;
                        }
                    }
                    ItemStack handItem = player.getItemInHand(hand);
                    if (player.gameMode.useItem(player, player.level(), handItem, hand).consumesAction()) {
                        ap.itemUseCooldown = 3;
                        return true;
                    }
                }
                return false;
            }

            @Override
            void inactiveTick(ServerPlayer player, Action action) {
                pack(player).itemUseCooldown = 0;
                player.releaseUsingItem();
            }
        },
        ATTACK {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                HitResult hit = getTarget(player);
                if (hit.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) hit;
                    if (!action.isContinuous) {
                        player.attack(entityHit.getEntity());
                        player.swing(InteractionHand.MAIN_HAND);
                    } else {
                        player.attack(entityHit.getEntity());
                        player.swing(InteractionHand.MAIN_HAND);
                    }
                    player.resetAttackStrengthTicker();
                    return true;
                }
                if (hit.getType() == HitResult.Type.BLOCK) {
                    EntityPlayerActionPack ap = pack(player);
                    if (ap.blockHitDelay > 0) {
                        ap.blockHitDelay--;
                        return false;
                    }
                    BlockHitResult blockHit = (BlockHitResult) hit;
                    BlockPos pos = blockHit.getBlockPos();
                    Direction side = blockHit.getDirection();
                    if (player.gameMode.getGameModeForPlayer().isCreative()) {
                        player.gameMode.handleBlockBreakAction(
                                pos,
                                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                                side,
                                player.level().getMaxY(),
                                -1
                        );
                        ap.blockHitDelay = 5;
                        player.swing(InteractionHand.MAIN_HAND);
                        return true;
                    }
                    BlockState state = player.level().getBlockState(pos);
                    if (ap.currentBlock == null || !ap.currentBlock.equals(pos)) {
                        if (ap.currentBlock != null) {
                            player.gameMode.handleBlockBreakAction(
                                    ap.currentBlock,
                                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                                    side,
                                    player.level().getMaxY(),
                                    -1
                            );
                        }
                        player.gameMode.handleBlockBreakAction(
                                pos,
                                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                                side,
                                player.level().getMaxY(),
                                -1
                        );
                        ap.currentBlock = pos;
                        ap.curBlockDamageMP = 0;
                    } else {
                        ap.curBlockDamageMP += state.getDestroyProgress(player, player.level(), pos);
                        if (ap.curBlockDamageMP >= 1) {
                            player.gameMode.handleBlockBreakAction(
                                    pos,
                                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                                    side,
                                    player.level().getMaxY(),
                                    -1
                            );
                            ap.currentBlock = null;
                            ap.blockHitDelay = 5;
                        }
                        player.level().destroyBlockProgress(-1, pos, (int) (ap.curBlockDamageMP * 10));
                    }
                    player.swing(InteractionHand.MAIN_HAND);
                    return true;
                }
                return false;
            }

            @Override
            void inactiveTick(ServerPlayer player, Action action) {
                EntityPlayerActionPack ap = pack(player);
                if (ap.currentBlock == null) {
                    return;
                }
                player.level().destroyBlockProgress(-1, ap.currentBlock, -1);
                player.gameMode.handleBlockBreakAction(
                        ap.currentBlock,
                        ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                        Direction.DOWN,
                        player.level().getMaxY(),
                        -1
                );
                ap.currentBlock = null;
            }
        },
        JUMP {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                if (action.limit == 1) {
                    if (player.onGround()) {
                        player.jumpFromGround();
                    }
                } else {
                    player.setJumping(true);
                }
                return false;
            }

            @Override
            void inactiveTick(ServerPlayer player, Action action) {
                player.setJumping(false);
            }
        };

        abstract boolean execute(ServerPlayer player, Action action);

        void inactiveTick(ServerPlayer player, Action action) {
        }

        void stop(ServerPlayer player, Action action) {
            inactiveTick(player, action);
        }

        private static EntityPlayerActionPack pack(ServerPlayer player) {
            if (player instanceof com.serverfakeplayer.nms.FakeServerPlayer fake) {
                return fake.actionPack();
            }
            throw new IllegalStateException("Action pack only available on fake players");
        }
    }

    public static final class Action {
        public boolean done;
        public final int limit;
        public final int interval;
        private int count;
        private int next;
        private final boolean isContinuous;

        private Action(int limit, int interval, boolean continuous) {
            this.limit = limit;
            this.interval = interval;
            this.next = interval;
            this.isContinuous = continuous;
        }

        public static Action once() {
            return new Action(1, 1, false);
        }

        public static Action continuous() {
            return new Action(-1, 1, true);
        }

        void tick(EntityPlayerActionPack actionPack, ActionType type) {
            next--;
            if (next <= 0) {
                if (interval == 1 && !isContinuous) {
                    type.inactiveTick(actionPack.player, this);
                }
                type.execute(actionPack.player, this);
                count++;
                if (count == limit) {
                    type.stop(actionPack.player, this);
                    done = true;
                    return;
                }
                next = interval;
            } else {
                type.inactiveTick(actionPack.player, this);
            }
        }
    }
}
