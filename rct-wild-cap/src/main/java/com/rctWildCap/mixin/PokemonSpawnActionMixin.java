package com.rctWildCap.mixin;

import com.rctWildCap.RctWildCapMod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(targets = "com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnAction", remap = false)
public abstract class PokemonSpawnActionMixin {

    @Inject(method = "createEntity()Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;", at = @At("HEAD"), cancellable = true, remap = false)
    private void rctWildCap_checkLevelCap(CallbackInfoReturnable<?> cir) {
        try {
            Object spawnablePos = this.getClass().getMethod("getSpawnablePosition").invoke(this);
            if (spawnablePos == null) return;
            Object world = spawnablePos.getClass().getMethod("getWorld").invoke(spawnablePos);
            if (!(world instanceof ServerWorld serverWorld)) return;
            Object levelRange = this.getClass().getMethod("getLevelRange").invoke(this);
            if (levelRange == null) return;
            int spawnLevelMax = (int) levelRange.getClass().getMethod("getLast").invoke(levelRange);
            Object blockPos = spawnablePos.getClass().getMethod("getPosition").invoke(spawnablePos);
            int bx = (int) blockPos.getClass().getMethod("getX").invoke(blockPos);
            int by = (int) blockPos.getClass().getMethod("getY").invoke(blockPos);
            int bz = (int) blockPos.getClass().getMethod("getZ").invoke(blockPos);
            Vec3d spawnPos = new Vec3d(bx, by, bz);
            List<ServerPlayerEntity> players = serverWorld.getPlayers();
            if (players.isEmpty()) return;
            Optional<ServerPlayerEntity> nearestOpt = players.stream()
                .filter(p -> p.getPos().squaredDistanceTo(spawnPos) <= 128.0 * 128.0)
                .min((a, b) -> Double.compare(
                    a.getPos().squaredDistanceTo(spawnPos),
                    b.getPos().squaredDistanceTo(spawnPos)));
            if (nearestOpt.isEmpty()) return;
            ServerPlayerEntity nearest = nearestOpt.get();
            Class<?> rctApiClass = Class.forName("com.gitlab.srcmc.rctmod.api.RctApi");
            Object trainerManager = rctApiClass.getMethod("getTrainerManager").invoke(null);
            if (trainerManager == null) return;
            Object playerData = trainerManager.getClass()
                .getMethod("getData", ServerPlayerEntity.class)
                .invoke(trainerManager, nearest);
            int levelCap = (int) playerData.getClass().getMethod("getLevelCap").invoke(playerData);
            if (spawnLevelMax >= levelCap) {
                RctWildCapMod.LOGGER.debug("[RCT Wild Cap] Spawn bloqué — niveau {} >= cap {} ({})",
                    spawnLevelMax, levelCap, nearest.getName().getString());
                cir.setReturnValue(null);
            }
        } catch (Exception e) {
            RctWildCapMod.LOGGER.debug("[RCT Wild Cap] Erreur: {}", e.getMessage());
        }
    }
}
