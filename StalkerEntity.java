package com.yandere.mod;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/** Arkanda duran siyah siluet. Ona bakınca (ya da 20 saniye sonra) dumanla kaybolur. */
public class StalkerEntity extends PathAwareEntity {
    private UUID ownerId;
    private int life = 0;
    private int watched = 0;

    public StalkerEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        setInvulnerable(true);
        setSilent(true);
        setAiDisabled(true);
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttrs() {
        return PathAwareEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 20);
    }

    public void setOwnerId(UUID id) { ownerId = id; }
    public UUID getOwnerId() { return ownerId; }

    @Override public boolean damage(DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canHaveStatusEffect(StatusEffectInstance e) { return false; }
    @Override public boolean cannotDespawn() { return true; }
    @Override public boolean canImmediatelyDespawn(double d) { return false; }
    @Override public Text getName() { return Text.literal("..."); }

    @Override public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        ServerPlayerEntity owner = ownerId == null ? null
                : ((ServerWorld) getWorld()).getServer().getPlayerManager().getPlayer(ownerId);
        if (owner == null || owner.getWorld() != getWorld()) { discard(); return; }

        life++;
        lookAtEntity(owner, 360f, 360f);

        Vec3d look = owner.getRotationVec(1f);
        Vec3d to = getEyePos().subtract(owner.getEyePos()).normalize();
        if (look.dotProduct(to) > 0.85 && owner.canSee(this)) watched++;
        else watched = Math.max(0, watched - 1);

        if (watched >= 12 || life > 400) vanish(owner, watched >= 12);
    }

    private void vanish(ServerPlayerEntity owner, boolean seen) {
        ServerWorld w = (ServerWorld) getWorld();
        w.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getBodyY(0.5), getZ(), 30, 0.3, 0.8, 0.3, 0.02);
        w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1f, 0.4f);
        if (seen) {
            owner.sendMessage(Text.literal(Lines.pick(Lines.SEEN)).formatted(Formatting.DARK_RED, Formatting.ITALIC), true);
            Memory.get(owner).log("Arkanda siluet olarak durdu, sen bakınca kayboldu");
        }
        discard();
    }
}
