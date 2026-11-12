package com.nicole.ocean;

/** Fixed simulation ticks, independent of display refresh rate. Long stalls never fast-forward danger. */
public final class FrameStepper {
    public static final float STEP = 1f / 60f;
    private double remainder;
    public int advance(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0) return 0;
        remainder += Math.min(seconds, .25);
        int ticks = Math.min(15, (int) Math.floor((remainder + 1e-9) * 60));
        remainder = Math.max(0, remainder - ticks / 60.0);
        return ticks;
    }
    public void reset() { remainder = 0; }
}
