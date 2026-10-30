package com.wasteconnect.engine;

import com.wasteconnect.dao.BinDao;
import com.wasteconnect.dao.BinDaoImpl;
import com.wasteconnect.dao.ZoneDao;
import com.wasteconnect.dao.ZoneDaoImpl;
import com.wasteconnect.model.Bin;
import com.wasteconnect.model.BinStatus;
import com.wasteconnect.model.Zone;
import com.wasteconnect.pattern.BinObservable;
import com.wasteconnect.pattern.BinObserver;
import com.wasteconnect.pattern.EventBus;
import com.wasteconnect.util.AppLogger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Novelty Engine & CO4 Multithreading:
 * Concurrent IoT Smart Bin telemetry simulator using ScheduledExecutorService.
 * Simulates real-time fill accumulation, festival surges, and dynamic threshold events.
 */
public class SmartBinSimulator {
    private final BinDao binDao;
    private final ZoneDao zoneDao;
    private final BinObservable observable = new BinObservable();

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile double speedMultiplier = 1.0;
    private final AtomicInteger tickCount = new AtomicInteger(0);

    // Thread-safe in-memory cache of active bins
    private final Map<String, Bin> binCache = new ConcurrentHashMap<>();
    private final Map<String, Zone> zoneCache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public SmartBinSimulator() {
        this(new BinDaoImpl(), new ZoneDaoImpl());
    }

    public SmartBinSimulator(BinDao binDao, ZoneDao zoneDao) {
        this.binDao = binDao;
        this.zoneDao = zoneDao;
        reloadCache();
    }

    public void addObserver(BinObserver observer) {
        observable.addObserver(observer);
    }

    public void removeObserver(BinObserver observer) {
        observable.removeObserver(observer);
    }

    public synchronized void reloadCache() {
        binCache.clear();
        zoneCache.clear();
        for (Zone z : zoneDao.findAll()) {
            zoneCache.put(z.getId(), z);
        }
        for (Bin b : binDao.findAll()) {
            binCache.put(b.getId(), b);
        }
    }

    public synchronized void start() {
        if (running.get()) return;
        reloadCache();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "IoT-SmartBin-Simulator");
            t.setDaemon(true);
            return t;
        });
        running.set(true);

        // Schedule telemetry tick every 2 seconds
        scheduler.scheduleAtFixedRate(this::simulationTick, 1, 2, TimeUnit.SECONDS);
        AppLogger.info("IoT Smart Bin Simulator started.");
    }

    public synchronized void stop() {
        if (!running.get()) return;
        running.set(false);
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        flushCacheToDb();
        AppLogger.info("IoT Smart Bin Simulator stopped.");
    }

    public boolean isRunning() {
        return running.get();
    }

    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = Math.max(0.1, Math.min(10.0, multiplier));
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    /**
     * "Festival Surge" event: Instantly simulates a major municipal celebration, market influx,
     * or weekend festival, causing an immediate 20%–45% surge across all bins!
     */
    public void triggerFestivalSurge() {
        AppLogger.warning("FESTIVAL SURGE triggered! Commercial accumulation rate surged across all zones.");
        List<Bin> updatedBins = new ArrayList<>();

        for (Bin bin : binCache.values()) {
            double surge = (15.0 + random.nextDouble() * 30.0);
            double oldFill = bin.getCurrentFillPercentage();
            double newFill = Math.min(100.0, oldFill + surge);
            BinStatus oldStatus = bin.getStatus();

            try {
                bin.setFillLevel(newFill);
            } catch (Exception ignored) {}

            observable.notifyFillUpdated(bin, oldFill, newFill);
            if (bin.getStatus() != oldStatus) {
                observable.notifyStatusChanged(bin, oldStatus, bin.getStatus());
            }
            updatedBins.add(bin);
        }

        CompletableFuture.runAsync(() -> {
            binDao.updateBatchFillLevels(updatedBins);
            publishFillEvents(updatedBins);
        });
    }

    /**
     * Resets all bins in the active cache to 15% fill and status NORMAL.
     */
    public void resetData() {
        List<Bin> resetList = new ArrayList<>();
        for (Bin bin : binCache.values()) {
            double oldFill = bin.getCurrentFillPercentage();
            bin.emptyBin();
            double fresh = 10.0 + random.nextDouble() * 15.0;
            try {
                bin.setFillLevel(fresh);
            } catch (Exception ignored) {}
            observable.notifyFillUpdated(bin, oldFill, fresh);
            observable.notifyStatusChanged(bin, BinStatus.WARNING, BinStatus.NORMAL);
            resetList.add(bin);
        }
        CompletableFuture.runAsync(() -> {
            binDao.updateBatchFillLevels(resetList);
            publishFillEvents(resetList);
        });
        AppLogger.info("All smart bins reset to baseline levels.");
    }

    /**
     * Periodic telemetry loop executing every 2 seconds.
     */
    private void simulationTick() {
        try {
            tickCount.incrementAndGet();
            List<Bin> modifiedBins = new ArrayList<>();
            List<Bin> overflowedBins = new ArrayList<>();

            for (Bin bin : binCache.values()) {
                Zone zone = zoneCache.get(bin.getZoneId());
                boolean isMarket = zone != null && zone.isMarketArea();

                // Base accumulation increment: 0.2% to 1.5% per tick * speedMultiplier
                double baseRate = isMarket ? (0.6 + random.nextDouble() * 1.4) : (0.2 + random.nextDouble() * 0.8);
                double delta = baseRate * speedMultiplier;

                double oldFill = bin.getCurrentFillPercentage();
                double newFill = Math.min(100.0, oldFill + delta);
                BinStatus oldStatus = bin.getStatus();

                try {
                    bin.setFillLevel(newFill);
                } catch (Exception ignored) {}

                if (oldFill < 100.0 && newFill >= 100.0) {
                    overflowedBins.add(bin);
                }

                if (newFill != oldFill) {
                    observable.notifyFillUpdated(bin, oldFill, newFill);
                }

                if (bin.getStatus() != oldStatus) {
                    observable.notifyStatusChanged(bin, oldStatus, bin.getStatus());
                }

                modifiedBins.add(bin);
            }

            binDao.updateBatchFillLevels(modifiedBins);
            EventBus bus = EventBus.getInstance();
            for (Bin bin : modifiedBins) {
                bus.publish(new EventBus.BinFillChanged(
                        bin.getId(), bin.getZoneId(), bin.getCurrentFillPercentage(), bin.getStatus().name()));
            }
            for (Bin bin : overflowedBins) {
                bus.publish(new EventBus.BinOverflow(bin.getId(), bin.getZoneId(), bin.getCurrentFillPercentage()));
            }
        } catch (Exception e) {
            AppLogger.severe("Exception inside simulationTick", e);
        }
    }

    public void flushCacheToDb() {
        binDao.updateBatchFillLevels(new ArrayList<>(binCache.values()));
    }

    private void publishFillEvents(List<Bin> bins) {
        EventBus bus = EventBus.getInstance();
        for (Bin bin : bins) {
            bus.publish(new EventBus.BinFillChanged(
                    bin.getId(), bin.getZoneId(), bin.getCurrentFillPercentage(), bin.getStatus().name()));
            if (bin.getStatus() == BinStatus.OVERFLOW) {
                bus.publish(new EventBus.BinOverflow(bin.getId(), bin.getZoneId(), bin.getCurrentFillPercentage()));
            }
        }
    }
}
