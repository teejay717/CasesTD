package btd.model.gacha;

import java.awt.Color;

/** How rare a tower is. Each rarity has a display color and a gem count (so it can be read without color). */
public enum Rarity {
    COMMON("Common", new Color(0x9EA7B8), 1),
    RARE("Rare", new Color(0x3B82F6), 2),
    EPIC("Epic", new Color(0xA855F7), 3),
    LEGENDARY("Legendary", new Color(0xFF7A1A), 4);

    private final String displayName;
    private final Color color;
    private final int gems;

    Rarity(String displayName, Color color, int gems) {
        this.displayName = displayName;
        this.color = color;
        this.gems = gems;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getColor() {
        return color;
    }

    public int getGems() {
        return gems;
    }
}
