package btd.model.gacha;

import java.awt.Color;

/**
 * [PURPOSE]       The 4 rarities: COMMON, RARE, EPIC, LEGENDARY. Each one has a display name, a color and a star count.
 * [OOP / PATTERN] Enum with fields and a constructor (an enum can hold data, not just names).
 * [CALLED BY]     btd/dao/TowerDAO.java (Rarity.valueOf("RARE")), btd/model/tower/TowerType.java,
 *                 btd/model/gacha/CaseDef.java, btd/core/CaseRNGManager.java, and the UI (colors, names, stars).
 * [CONNECTS TO]   java.awt.Color.
 * [DEFENSE NOTE]  Order matters: CaseRNGManager loops Rarity.values() from COMMON to LEGENDARY.
 *                 The enum names must match the text in the towers.rarity column exactly.
 */
public enum Rarity {
    COMMON("Common", new Color(0x9EA7B8), 1),
    RARE("Rare", new Color(0x3B82F6), 2),
    EPIC("Epic", new Color(0xA855F7), 3),
    LEGENDARY("Legendary", new Color(0xFF7A1A), 4);

    private final String displayName;
    private final Color color;
    private final int gems;

    /**
     * [PURPOSE]       Stores the name, color and stars for each constant.
     * [OOP / PATTERN] Enum Constructor (always private; you cannot call "new Rarity(...)").
     * [CALLED BY]     Java itself, once for each of the 4 constants above, when the class loads.
     * [CONNECTS TO]   Internal fields.
     */
    Rarity(String displayName, Color color, int gems) {
        this.displayName = displayName;
        this.color = color;
        this.gems = gems;
    }

    /** [PURPOSE] Human-readable display label (e.g. "Legendary"). */
    public String getDisplayName() {
        return displayName;
    }

    /** [PURPOSE] Brand UI accent color used for cards, badges, and glows. */
    public Color getColor() {
        return color;
    }

    /** [PURPOSE] Star count, 1 (Common) to 4 (Legendary). ShopOverlay.paintResult() draws it as "*" characters. */
    public int getGems() {
        return gems;
    }
}
