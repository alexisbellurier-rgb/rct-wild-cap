package com.rctWildCap;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RctWildCapMod implements ModInitializer {

    public static final String MOD_ID = "rct_wild_cap";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[RCT Wild Cap] Mod initialisé — les Pokémon sauvages respecteront le level cap RCT.");
    }
}
