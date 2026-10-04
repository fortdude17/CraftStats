package com.craftstats.common.logic;

import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.stats.WorldStats;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.WeakHashMap;

/** World settings that are applied every tick: day length and weather. */
public final class WorldHooks {

    private record Clock(long lastTime, double carry) {}

    private static final Map<ServerLevel, Clock> CLOCKS = new WeakHashMap<>();

    private WorldHooks() {}

    public static void levelTick(ServerLevel level) {
        WorldStats w = StatRegistry.world();
        if (w == null) { CLOCKS.remove(level); return; }
        if (w.alwaysClear && level.isRaining() && level.getGameTime() % 20 == 0)
            level.setWeatherParameters(12000, 0, false, false);
        dayLength(level, w);
    }

    /**
     * Longer or shorter days. Works on whatever the game itself does with time: if the
     * clock moved since last tick (daylight cycle on), move it a bit more or less.
     */
    private static void dayLength(ServerLevel level, WorldStats w) {
        long now = level.getDayTime();
        Clock c = CLOCKS.get(level);
        if (w.dayLengthMultiplier == null || w.dayLengthMultiplier == 1.0) {
            CLOCKS.put(level, new Clock(now, 0));
            return;
        }
        double carry = c == null ? 0 : c.carry();
        if (c != null && now - c.lastTime() == 1) {
            // Normal speed is 1 per tick; a day length of x means 1/x per tick.
            carry += 1.0 / Math.max(0.05, w.dayLengthMultiplier) - 1.0;
            long whole = (long) carry;
            if (whole != 0) {
                now += whole;
                carry -= whole;
                level.setDayTime(now);
            }
        }
        CLOCKS.put(level, new Clock(now, carry));
    }
}
