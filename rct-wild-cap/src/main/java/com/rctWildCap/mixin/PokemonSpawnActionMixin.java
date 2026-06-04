package com.rctWildCap.mixin;

import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnAction;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.gitlab.srcmc.rctmod.api.RctApi;
import com.gitlab.srcmc.rctmod.api.service.ITrainerManager;
import com.rctWildCap.RctWildCapMod;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import kotlin.ranges.IntRange;
import java.util.List;
import java.util.Optional;

@Mixin(targets = "com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnAction",
       remap = false)
public abstract class PokemonSpawnActionMixin {

    @Shadow public abstract SpawnablePosition getSpawnablePosition();
    @Shadow public abstract IntRange getLevelRange();

    @Inject(method = "createEntity()Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void rctWildCap_checkLevelCap(CallbackInfoReturnable<PokemonEntity> cir) {

        // 1. Position et monde du spawn
        SpawnablePosition spawnablePos = getSpawnablePosition();
        if (spawnablePos == null) return;

        if (!(spawnablePos.getWorld() instanceof ServerWorld serverWorld)) return;

        // 2. Niveau maximum du Pokémon qui va spawner
        IntRange levelRange = getLevelRange();
        if (levelRange == null) return;
        int spawnLevelMax = levelRange.getLast();

        // 3. Position du bloc de spawn
        BlockPos blockPos = spawnablePos.getPosition();
        Vec3d spawnPos = new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        // 4. Joueur le plus proche dans un rayon de 128 blocs
        List<ServerPlayerEntity> players = serverWorld.getPlayers();
        if (players.isEmpty()) return;

        Optional<ServerPlayerEntity> nearestOpt = players.stream()
            .filter(p -> p.getPos().squaredDistanceTo(spawnPos) <= 128.0 * 128.0)
            .min((a, b) -> Double.compare(
                a.getPos().squaredDistanceTo(spawnPos),
                b.getPos().squaredDistanceTo(spawnPos)
            ));

        if (nearestOpt.isEmpty()) return;
        ServerPlayerEntity nearest = nearestOpt.get();

        // 5. Level cap RCT
        int levelCap;
        try {
            ITrainerManager tm = RctApi.getTrainerManager();
            if (tm == null) return;
            levelCap = tm.getData(nearest).getLevelCap();
        } catch (Exception e) {
            RctWildCapMod.LOGGER.debug("[RCT Wild Cap] Erreur lecture level cap pour {}: {}",
                nearest.getName().getString(), e.getMessage());
            return;
        }

        // 6. Bloquer si niveau max >= level cap
        if (spawnLevelMax >= levelCap) {
            RctWildCapMod.LOGGER.debug(
                "[RCT Wild Cap] Spawn bloqué — niveau max {} >= cap {} ({})",
                spawnLevelMax, levelCap, nearest.getName().getString());
            cir.setReturnValue(null);
        }
    }
}
