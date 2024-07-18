package network.darkhelmet.prism.actions.entity;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import network.darkhelmet.prism.actions.typeadapter.BoolIgnoreTrueAdapter;
import network.darkhelmet.prism.utils.EntityUtils;
import network.darkhelmet.prism.utils.MiscUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntitySerializer {
    //@todo remove alternates after 2.1.7 release
    @SerializedName(value = "ad", alternate = {"isAdult"})
    @JsonAdapter(BoolIgnoreTrueAdapter.class)
    protected Boolean isAdult = null;
    @SerializedName(value = "sit", alternate = {"sitting"})
    protected Boolean sitting = null;

    @SerializedName(value = "en", alternate = {"entity_name", "entityName"})
    protected String entityName = null;

    @SerializedName(value = "cn", alternate = {"custom_name", "customName"})
    protected String customName = null;

    @SerializedName(value = "to", alternate = {"taming_owner", "tamingOwner"})
    protected String tamingOwner = null;
    @SerializedName(value = "nc", alternate = {"newColor"})
    protected String newColor = null;

    @SerializedName(value = "cd", alternate = {"custom_desc","customDesc"})
    protected String customDesc = null;

    public final String getEntityName() {
        return entityName;
    }

    public final String customDesc() {
        return customDesc;
    }

    // Le sigh
    public final void setNewColor(String color) {
        newColor = color;
    }

    /**
     * Serialize entity.
     * @param entity Entity.
     */
    public final void serialize(Entity entity) {
        entityName = entity.getType().name().toLowerCase();

        // Get custom name
        customName = entity.getCustomName();

        // Get animal age
        if (entity instanceof Ageable) {
            isAdult = ((Ageable) entity).isAdult();
        }

        // Owner
        if (entity instanceof Tameable) {
            final Tameable mob = (Tameable) entity;
            if (mob.getOwner() != null) {
                tamingOwner = mob.getOwner().getUniqueId().toString();
            } else if (mob.isTamed()) {
                tamingOwner = "-none-";
            }
        }

        // Sitting
        if (entity instanceof Sittable) {
            sitting = ((Sittable) entity).isSitting();
        }

        EntityDamageEvent damageEvent = entity.getLastDamageCause();

        // Saves us the null check
        if (damageEvent instanceof EntityDamageByEntityEvent && !damageEvent.isCancelled()
                && damageEvent.getDamage() > ((LivingEntity) entity).getHealth()) {
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) damageEvent;

            if (e.getDamager() instanceof Projectile) {
                customDesc = EntityUtils.getCustomProjectileDescription((Projectile) e.getDamager());
            }
        }

        serializer(entity);
    }

    protected void serializer(Entity entity) {
    }

    /**
     * Deserialize.
     * @param entity Entity
     */
    public final void deserialize(Entity entity) {
        // Get custom name
        if (entity instanceof LivingEntity && customName != null) {
            entity.setCustomName(customName);
        }

        // Get animal age
        if (entity instanceof Ageable) {
            final Ageable age = (Ageable) entity;
            if (Boolean.FALSE.equals(isAdult)) {
                age.setBaby();
            } else {
                age.setAdult();
            }
        }
        // Owner
        if (entity instanceof Tameable) {
            ((Tameable) entity).setOwner(EntityUtils.offlineOf(tamingOwner));
        }

        // Sitting
        if (entity instanceof Sittable) {
            ((Sittable) entity).setSitting(Boolean.TRUE.equals(sitting));
        }

        deserializer(entity);
    }

    protected void deserializer(Entity entity) {
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        if (tamingOwner != null) {

            OfflinePlayer player = EntityUtils.offlineOf(tamingOwner);
            if (player != null) {
                String str = player.getName() + "'s ";
                sb.append(str);
                index = str.length();
            }
        }

        if (Boolean.FALSE.equals(isAdult)) {
            sb.append("baby ");
        }

        sb.append(MiscUtils.niceName(entityName));

        if (newColor != null) {
            sb.append(' ').append(MiscUtils.niceName(newColor));
        }

        if (customName != null) {
            sb.append(" named ").append(customName);
        }

        niceName(sb, index);
        return sb.toString();
    }

    protected void niceName(StringBuilder sb, int start) {
    }
}
