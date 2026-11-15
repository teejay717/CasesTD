package btd.model.gacha;

import java.awt.Color;

/**
 * [PURPOSE]       Tier enumeration for tower and drop rarity, defining color accents and gem counts.
 * [OOP / PATTERN] Java Enum / Rich Domain Object Pattern.
 * [CALLED BY]     btd/model/gacha/CaseDef.java, btd/model/tower/TowerType.java,
 *                 btd/dao/TowerDAO.java, btd/ui/screens/SidePanel.java.
 * [CONNECTS TO]   btd/model/gacha/CaseDef.java, java.awt.Color.
 * [DEFENSE NOTE]  Pairs each rarity with an exact UI theme color and gem count (1-4) for visual accessibility.
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
     * [PURPOSE]       Initializes enum constant properties.
     * [OOP / PATTERN] Enum Constructor.
     * [CALLED BY]     Enum instantiation.
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

    /** [PURPOSE] Numerical gem count rating (1 for Common up to 4 for Legendary). */
    public int getGems() {
        return gems;
    }
}
