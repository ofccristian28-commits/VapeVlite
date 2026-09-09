package br.vapevlite;

public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String id, double value, double min, double max, double step) {
        super(id, value);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }

    public void increment() {
        setValue(Math.min(max, round(getValue() + step)));
    }

    public void decrement() {
        setValue(Math.max(min, round(getValue() - step)));
    }

    private double round(double v) {
        double n = Math.round(v / step) * step;
        return Math.round(n * 100.0D) / 100.0D;
    }
}
