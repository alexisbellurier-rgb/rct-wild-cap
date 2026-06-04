package com.rctWildCap.mixin;

import com.cobblemon.mod.common.api.spawning.context.SpawningContext;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.gitlab.srcmc.rctmod.api.RctApi;
import com.gitlab.srcmc.rctmod.api.service.ITrainerManager;
import com.rctWildCap.RctWildCapMod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/**
 * Cible : PokemonSpawnAction (sous-classe concrète de SingleEntitySpawnAction).
 *
 * On injecte en HEAD de createEntity() — c'est la méthode qui construit
 * réellement le PokemonEntity. Si on retourne null depuis là, Cobblemon
 * abandonne le spawn proprement (comportement documenté dans SingleEntitySpawnAction#run).
 *
 * remap=false : les classes Cobblemon sont distribuées déjà mappées (intermediary),
 * pas besoin du remapping Yarn pour les noms Cobblemon.
 */
@Mixin(targets = "com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnAction",
       remap = false)
public abstract class PokemonSpawnActionMixin {

    // ── Accès aux champs hérités de SpawnAction<CTX, D, E> ──────────────────

    /** Le contexte de spawn : contient le monde, la position, etc. */
    @Shadow public abstract SpawningContext getCtx();

    /** Le SpawnDetail : contient le levelRange, l'espèce, etc. */
    @Shadow public abstract SpawnDetail getDetail();

    // ────────────────────────────────────────────────────────────────────────

    @Inject(method = "createEntity",
            at = @At("HEAD"),
            cancellable = true)
    private void rctWildCap_filterByLevelCap(CallbackInfoReturnable<PokemonEntity> cir) {

        // 1. Contexte de spawn
        SpawningContext ctx = getCtx();
        if (ctx == null) return;

        if (!(ctx.getWorld() instanceof ServerWorld serverWorld)) return;

        // 2. Détail de spawn → niveau maximal possible
        SpawnDetail detail = getDetail();
        if (!(detail instanceof PokemonSpawnDetail pokemonDetail)) return;

        // levelRange est un IntRange kotlin ; getLast() = borne haute
        int spawnLevelMax = pokemonDetail.getLevelRange().getLast();

        // 3. Position du spawn
        Vec3d spawnPos = new Vec3d(ctx.getPosition().getX(),
                                   ctx.getPosition().getY(),
                                   ctx.getPosition().getZ());

        // 4. Joueur le plus proche (dans un rayon de 128 blocs)
        List<ServerPlayerEntity> players = serverWorld.getPlayers();
        if (players.isEmpty()) return;

        Optional<ServerPlayerEntity> nearestOpt = players.stream()
            .filter(p -> p.getPos().squaredDistanceTo(spawnPos) <= 128.0 * 128.0)
            .min((a, b) -> Double.compare(
                a.getPos().squaredDistanceTo(spawnPos),
                b.getPos().squaredDistanceTo(spawnPos)
            ));

        if (nearestOpt.isEmpty()) return; // aucun joueur à portée → spawn libre
        ServerPlayerEntity nearest = nearestOpt.get();

        // 5. Level cap RCT du joueur
        int levelCap;
        try {
            ITrainerManager tm = RctApi.getTrainerManager();
            if (tm == null) return; // RCT absent → pas de restriction

            // TrainerPlayerData.getLevelCap() renvoie le cap actuel du joueur
            levelCap = tm.getData(nearest).getLevelCap();
        } catch (Exception e) {
            RctWildCapMod.LOGGER.debug(
                "[RCT Wild Cap] getLevelCap() indisponible pour {} : {}",
                nearest.getName().getString(), e.getMessage());
            return; // sécurité : on laisse spawner plutôt que de planter
        }

        // 6. Décision
        if (spawnLevelMax >= levelCap) {
            RctWildCapMod.LOGGER.debug(
                "[RCT Wild Cap] Spawn bloqué — niveau max {} >= cap {} du joueur {}",
                spawnLevelMax, levelCap, nearest.getName().getString());
            cir.setReturnValue(null); // null = Cobblemon abandonne le spawn
        }
    }
}
