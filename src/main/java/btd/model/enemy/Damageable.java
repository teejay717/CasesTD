package btd.model.enemy;

/** Something that can be hurt and destroyed, like an enemy. */
public interface Damageable {

    void takeDamage(double amount);

    boolean isDead();
}
