package com.gmail.nossr50.listeners;

import com.gmail.nossr50.mcMMO;
import org.bukkit.Chunk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Arrays;

public class ChunkListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        final Chunk unloadingChunk = event.getChunk();
        Arrays.stream(unloadingChunk.getEntities())
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .forEach(livingEntity -> mcMMO.getTransientEntityTracker().removeTrackedEntity(livingEntity));
    }
}
