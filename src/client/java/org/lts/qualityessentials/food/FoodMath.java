package org.lts.qualityessentials.food;

public final class FoodMath {
    public static final int MAX_FOOD = 20;

    private FoodMath() {
    }

    public static Result afterEating(int food, float saturation, int nutrition, float saturationGain) {
        int newFood = Math.min(MAX_FOOD, food + Math.max(0, nutrition));
        float newSaturation = Math.min(newFood, saturation + Math.max(0f, saturationGain));
        return new Result(newFood, Math.max(saturation, newSaturation));
    }

    public static Fill iconFill(float value, int icon) {
        float inIcon = value - icon * 2f;
        if (inIcon >= 2f) {
            return Fill.FULL;
        }
        if (inIcon >= 1f) {
            return Fill.HALF;
        }
        return Fill.EMPTY;
    }

    public static boolean canEat(int food, boolean canAlwaysEat, boolean creative) {
        return !creative && (canAlwaysEat || food < MAX_FOOD);
    }

    public record Result(int food, float saturation) {
    }

    public enum Fill {
        EMPTY,
        HALF,
        FULL
    }
}
