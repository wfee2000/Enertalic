package com.wfee;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginManager;

import javax.annotation.Nonnull;

public class Enertalic extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public Enertalic(@Nonnull JavaPluginInit init) {
        super(init);
        PluginManager.get().load(new EnergyModule(init).getIdentifier());
    }

    @Override
    protected void setup() {
        super.setup();
        LOGGER.atInfo().log("Enertalic plugin has been loaded");
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Enertalic plugin started");
    }
}