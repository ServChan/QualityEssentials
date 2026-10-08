package org.lts.qualityessentials.keyboard.layout;

public record KeyCap(String label, boolean mouse, int code, float x, float y, float width, float height) {
    public boolean contains(float px, float py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    public boolean overlaps(KeyCap other) {
        return x < other.x + other.width && other.x < x + width && y < other.y + other.height && other.y < y + height;
    }
}
