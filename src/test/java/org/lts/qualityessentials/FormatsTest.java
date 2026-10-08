package org.lts.qualityessentials;

import org.junit.jupiter.api.Test;
import org.lts.qualityessentials.effects.EffectFormat;
import org.lts.qualityessentials.equipment.DurabilityFormat;
import org.lts.qualityessentials.food.FoodMath;
import org.lts.qualityessentials.lookat.LookAtFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatsTest {
    @Test
    void eatingCapsHungerAndSaturation() {
        FoodMath.Result steak = FoodMath.afterEating(12, 2f, 8, 12.8f);
        assertEquals(20, steak.food());
        assertEquals(14.8f, steak.saturation(), 0.001f);
        FoodMath.Result capped = FoodMath.afterEating(18, 15f, 8, 12.8f);
        assertEquals(20, capped.food());
        assertEquals(20f, capped.saturation(), 0.001f);
        FoodMath.Result small = FoodMath.afterEating(4, 0f, 2, 1.2f);
        assertEquals(6, small.food());
        assertEquals(1.2f, small.saturation(), 0.001f);
    }

    @Test
    void saturationNeverDropsWhenFoodIsCapped() {
        FoodMath.Result result = FoodMath.afterEating(20, 18f, 4, 1f);
        assertEquals(19f, result.saturation(), 0.001f);
        FoodMath.Result already = FoodMath.afterEating(2, 5f, 1, 0f);
        assertEquals(5f, already.saturation(), 0.001f);
    }

    @Test
    void iconFillMatchesVanillaHalves() {
        assertEquals(FoodMath.Fill.FULL, FoodMath.iconFill(7f, 0));
        assertEquals(FoodMath.Fill.FULL, FoodMath.iconFill(7f, 2));
        assertEquals(FoodMath.Fill.HALF, FoodMath.iconFill(7f, 3));
        assertEquals(FoodMath.Fill.EMPTY, FoodMath.iconFill(7f, 4));
        assertEquals(FoodMath.Fill.HALF, FoodMath.iconFill(1.6f, 0));
    }

    @Test
    void canEatRules() {
        assertTrue(FoodMath.canEat(19, false, false));
        assertFalse(FoodMath.canEat(20, false, false));
        assertTrue(FoodMath.canEat(20, true, false));
        assertFalse(FoodMath.canEat(5, false, true));
    }

    @Test
    void effectDurations() {
        assertEquals("45s", EffectFormat.duration(45 * 20, false));
        assertEquals("3:05", EffectFormat.duration(185 * 20, false));
        assertEquals("1:00", EffectFormat.duration(60 * 20, false));
        assertEquals("2h", EffectFormat.duration(7300 * 20, false));
        assertEquals("∞", EffectFormat.duration(5, true));
        assertEquals("0s", EffectFormat.duration(-5, false));
    }

    @Test
    void effectLevels() {
        assertEquals("", EffectFormat.level(0));
        assertEquals("II", EffectFormat.level(1));
        assertEquals("X", EffectFormat.level(9));
        assertEquals("12", EffectFormat.level(11));
        assertEquals(0xFFFF6B6B, EffectFormat.durationColor(100, false));
        assertEquals(0xFFFFFFFF, EffectFormat.durationColor(100, true));
    }

    @Test
    void durabilityText() {
        assertEquals("1500", DurabilityFormat.text(1561, 61, false));
        assertEquals("96%", DurabilityFormat.text(1561, 61, true));
        assertEquals("0", DurabilityFormat.text(100, 150, false));
        assertEquals(1f, DurabilityFormat.fraction(0, 0));
        assertEquals(0xFFFF5050, DurabilityFormat.color(0.05f));
        assertEquals(0xFF7CFC7C, DurabilityFormat.color(0.9f));
        assertEquals("12k", DurabilityFormat.count(12345));
        assertEquals("640", DurabilityFormat.count(640));
    }

    @Test
    void lookAtHelpers() {
        assertEquals(43, LookAtFormat.percent(3, 0, 7));
        assertEquals(100, LookAtFormat.percent(0, 0, 0));
        assertEquals("7.5 / 20.0", LookAtFormat.hearts(7.5f, 20f));
        assertEquals("14.2", LookAtFormat.oneDecimal(LookAtFormat.horseSpeed(0.3375)));
        assertEquals("5.3", LookAtFormat.oneDecimal(LookAtFormat.horseJumpHeight(1.0)));
        assertTrue(LookAtFormat.shows("age"));
        assertFalse(LookAtFormat.shows("facing"));
    }
}
