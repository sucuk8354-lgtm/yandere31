package com.yandere.mod;

import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.*;

public class YandereEntity extends PathAwareEntity {
    private static final TrackedData<Boolean> PSYCHO =
            DataTracker.registerData(YandereEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Random RNG = new Random();

    private UUID ownerId;
    public double jealousy = 0;
    private int psychoTicks = 0;
    private int pendingKill = 0;

    public YandereEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        setInvulnerable(true);
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttrs() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
    }

    @Override protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(PSYCHO, false);
    }

    @Override protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
    }

    public boolean isPsycho() { return dataTracker.get(PSYCHO); }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID id) { ownerId = id; }
    public void requestKill() { if (pendingKill == 0) pendingKill = 60; }

    // ---- Dokunulmazlık ----
    @Override public boolean damage(DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canHaveStatusEffect(StatusEffectInstance effect) { return false; }
    @Override public boolean isFireImmune() { return true; }
    @Override public boolean cannotDespawn() { return true; }
    @Override public boolean canImmediatelyDespawn(double d) { return false; }
    @Override public boolean isCustomNameVisible() { return true; }
    @Override public Text getName() { return Text.literal(Config.name()); }

    private ServerPlayerEntity getOwner() {
        if (ownerId == null || getWorld().isClient) return null;
        return ((ServerWorld) getWorld()).getServer().getPlayerManager().getPlayer(ownerId);
    }

    @Override public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        ServerPlayerEntity owner = getOwner();
        if (owner == null) return;
        if (owner.getWorld() != getWorld()) { if (age % 100 == 0) discard(); return; }

        boolean busy = isPsycho() && psychoTicks > 120;
        if (!busy) {
            double d = squaredDistanceTo(owner);
            if (d > 256) teleportNear(owner);
            else if (d > 9 && age % 10 == 0) getNavigation().startMovingTo(owner, 1.2);
            else if (d < 4) getNavigation().stop();
            getLookControl().lookAt(owner, 30f, 30f);
        }

        if (pendingKill > 0 && --pendingKill == 0) killNearestVillager(owner);

        if (isPsycho()) { tickMeltdown(owner); return; }

        if (age % 20 == 0) {
            int near = getWorld().getEntitiesByClass(VillagerEntity.class,
                    owner.getBoundingBox().expand(10), Entity::isAlive).size();
            int pets = getWorld().getEntitiesByClass(TameableEntity.class, owner.getBoundingBox().expand(4),
                    t -> t.isTamed() && owner.getUuid().equals(t.getOwnerUuid())).size();
            Memory.get(owner); // hafızayı yükle
            if (near > 0 || pets > 0)
                jealousy += Config.I.jealousyPerVillagerPerSecond * near + 0.3 * pets;
            else jealousy = Math.max(0, jealousy - Config.I.jealousyDecayPerSecond);
            jealousy = Math.min(jealousy, 120);

            if (pets > 0 && jealousy >= 35 && RNG.nextInt(20) == 0) {
                owner.sendMessage(Text.literal(Lines.pick(Lines.PET)).formatted(Formatting.DARK_RED, Formatting.ITALIC), true);
                Memory.get(owner).log("Evcil hayvanına fazla ilgi gösterdiğin için kıskandı");
            }
            if (jealousy >= 60 && RNG.nextInt(15) == 0) creep(owner);
            if (jealousy >= 40 && RNG.nextInt(25) == 0) trySpawnStalker(owner);
            if (jealousy >= Config.I.meltdownThreshold) startMeltdown(owner);
        }
    }

    /** Kalp atışı + ense hizasında fısıltı. */
    private void creep(ServerPlayerEntity owner) {
        ServerWorld w = owner.getServerWorld();
        w.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                SoundCategory.AMBIENT, 1f, 0.8f);
        Vec3d back = owner.getRotationVec(1f).multiply(-1).add(0, 0.2, 0);
        w.playSound(null, owner.getX() + back.x, owner.getEyeY() + back.y * 0.3, owner.getZ() + back.z,
                SoundEvents.ENTITY_VEX_AMBIENT, SoundCategory.HOSTILE, 0.8f, 0.5f);
        owner.sendMessage(Text.literal(Lines.pick(Lines.CREEP)).formatted(Formatting.DARK_RED, Formatting.ITALIC), true);
    }

    /** Oyuncunun arkasında siyah siluet doğurur; ona bakınca kaybolur. */
    public void trySpawnStalker(ServerPlayerEntity owner) {
        ServerWorld w = (ServerWorld) getWorld();
        if (!w.getEntitiesByType(YandereMod.STALKER, s -> owner.getUuid().equals(s.getOwnerId())).isEmpty()) return;
        Vec3d back = owner.getRotationVec(1f).multiply(1, 0, 1).normalize().multiply(-1);
        double dist = 10 + RNG.nextInt(7);
        BlockPos top = w.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.ofFloored(owner.getX() + back.x * dist, owner.getY(), owner.getZ() + back.z * dist));
        if (Math.abs(top.getY() - owner.getY()) > 6) return;
        StalkerEntity s = YandereMod.STALKER.create(w);
        if (s == null) return;
        s.setOwnerId(owner.getUuid());
        s.refreshPositionAndAngles(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 0, 0);
        w.spawnEntity(s);
    }

    private void teleportNear(ServerPlayerEntity o) {
        refreshPositionAndAngles(o.getX() + 1.2, o.getY(), o.getZ() + 1.2, o.getYaw(), 0);
        getNavigation().stop();
    }

    public void startMeltdown(ServerPlayerEntity owner) {
        if (isPsycho()) return;
        dataTracker.set(PSYCHO, true);
        psychoTicks = 240;
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 120, 0));
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0));
        owner.getServerWorld().playSound(null, owner.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_SCREAM,
                SoundCategory.HOSTILE, 1f, 0.5f);
        owner.sendMessage(Text.literal("<" + Config.name() + "> ").formatted(Formatting.DARK_RED)
                .append(Text.literal(Lines.pick(Lines.MELTDOWN)).formatted(Formatting.RED, Formatting.OBFUSCATED)));
        ChatAI.say(owner, Lines.pick(Lines.MELTDOWN));
        Memory.get(owner).log("Kıskançlıktan çıldırdı (bozulma)");
    }

    private static String glitch() {
        String chars = "#@%&?!01XZ▓▒░";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 18; i++) sb.append(chars.charAt(RNG.nextInt(chars.length())));
        return sb.toString();
    }

    private void tickMeltdown(ServerPlayerEntity owner) {
        psychoTicks--;
        ServerWorld w = (ServerWorld) getWorld();
        w.spawnParticles(new DustParticleEffect(new Vector3f(0.7f, 0f, 0f), 1.2f),
                getX(), getBodyY(0.5), getZ(), 6, 0.3, 0.6, 0.3, 0.01);
        if (age % 8 == 0) {
            owner.sendMessage(Text.literal(glitch()).formatted(Formatting.RED, Formatting.OBFUSCATED), true);
            w.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                    SoundCategory.HOSTILE, 1f, 0.5f + RNG.nextFloat());
        }
        if (psychoTicks == 200 && Config.I.enableVillagerKills) killNearestVillager(owner);
        if (psychoTicks == 120) teleportNear(owner);
        if (psychoTicks <= 0) calm(owner);
    }

    /** En yakın köylüyü evine götürüp öldürür ve zemine kan bırakır. */
    private void killNearestVillager(ServerPlayerEntity owner) {
        ServerWorld w = (ServerWorld) getWorld();
        List<VillagerEntity> list = w.getEntitiesByClass(VillagerEntity.class,
                owner.getBoundingBox().expand(48), Entity::isAlive);
        if (list.isEmpty()) return;
        VillagerEntity v = list.stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(owner))).get();

        BlockPos pos = v.getBlockPos();
        Optional<GlobalPos> home = v.getBrain().getOptionalMemory(MemoryModuleType.HOME);
        if (home.isPresent() && home.get().getDimension() == w.getRegistryKey()) {
            BlockPos h = home.get().getPos();
            v.getNavigation().stop();
            v.refreshPositionAndAngles(h.getX() + 0.5, h.getY(), h.getZ() + 0.5, v.getYaw(), v.getPitch());
            pos = h;
        }
        refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, getYaw(), 0);
        getNavigation().stop();

        w.playSound(null, pos, SoundEvents.ENTITY_VILLAGER_DEATH, SoundCategory.NEUTRAL, 1.2f, 0.7f);
        v.damage(w.getDamageSources().magic(), Float.MAX_VALUE);
        placeBlood(w, pos);

        Memory.PlayerData d = Memory.get(owner);
        d.villagersKilled++;
        d.log("Bir köylüyü evinde öldürdü, zemine kan bıraktı");
        Memory.save(owner);
    }

    private static void placeBlood(ServerWorld world, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (RNG.nextInt(3) == 0) continue;
                BlockPos p = center.add(dx, 0, dz);
                BlockPos below = p.down();
                if (world.getBlockState(p).isAir()
                        && world.getBlockState(below).isSideSolidFullSquare(world, below, Direction.UP)) {
                    world.setBlockState(p, Blocks.REDSTONE_WIRE.getDefaultState());
                }
            }
        }
        world.spawnParticles(new net.minecraft.particle.BlockStateParticleEffect(ParticleTypes.BLOCK,
                Blocks.REDSTONE_BLOCK.getDefaultState()),
                center.getX() + 0.5, center.getY() + 0.6, center.getZ() + 0.5, 40, 0.4, 0.3, 0.4, 0.05);
    }

    public void calm(ServerPlayerEntity owner) {
        dataTracker.set(PSYCHO, false);
        psychoTicks = 0;
        jealousy = 20;
        ChatAI.say(owner, Lines.pick(Lines.CALM));
        Memory.get(owner).log("Sakinleşti, özür diledi");
        Memory.save(owner);
    }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (ownerId != null) nbt.putUuid("Owner", ownerId);
        nbt.putDouble("Jealousy", jealousy);
    }

    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("Owner")) ownerId = nbt.getUuid("Owner");
        jealousy = nbt.getDouble("Jealousy");
    }
}
