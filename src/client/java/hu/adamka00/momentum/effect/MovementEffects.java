package hu.adamka00.momentum.effect;

import hu.adamka00.momentum.config.MomentumConfig;
import hu.adamka00.momentum.network.payload.EffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class MovementEffects {
    private MovementEffects() { }
    public static void play(Minecraft client, EffectPayload payload, MomentumConfig.Client preferences) {
        if (client.level == null) return;
        var entity = client.level.getEntity(payload.entityId());
        if (entity == null) return;
        var particle = payload.kind() == 3 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.CLOUD;
        int count = (payload.kind() == 5 || payload.kind() == 9) ? 2 : 6;
        if (payload.particles() && preferences.particles) {
            var random = entity.getRandom();
            for (int i = 0; i < count; i++) {
                client.level.addParticle(particle, entity.getX() + (random.nextDouble() - 0.5) * 0.5,
                    entity.getY() + 0.12, entity.getZ() + (random.nextDouble() - 0.5) * 0.5, 0, 0.025, 0);
            }
        }
        if (payload.sounds() && preferences.sounds) {
            var sound = payload.kind() == 6 ? SoundEvents.PHANTOM_FLAP : payload.kind() == 4 ? SoundEvents.BREEZE_SHOOT
                : payload.kind() == 3 ? SoundEvents.PHANTOM_FLAP : SoundEvents.BREEZE_JUMP;
            client.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, 0.25f, 1.3f, false);
        }
    }
}
