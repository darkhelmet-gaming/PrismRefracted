package network.darkhelmet.prism.actions.entity;

import com.google.gson.annotations.SerializedName;
import network.darkhelmet.prism.utils.ItemUtils;
import network.darkhelmet.prism.utils.MiscUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;

public class AbstractHorseSerializer extends EntitySerializer {
    @SerializedName(value = "c", alternate = {"horseColor"})
    protected String horseColor = null;
    @SerializedName(value = "s", alternate = {"style"})
    protected String style = null;
    @SerializedName(value = "sd", alternate = {"saddle"})
    protected String saddle = null;
    @SerializedName(value = "a", alternate = {"armor"})
    protected String armor = null;
    @SerializedName(value = "ch", alternate = {"chest"})
    protected Boolean chest = null;
    @SerializedName(value = "d", alternate = {"dom"})
    protected int dom = 0;
    @SerializedName(value = "md", alternate = {"maxDom"})
    protected int maxDom = 20;
    @SerializedName(value = "j", alternate = {"jump"})
    protected double jump = 1.0;
    @SerializedName(value = "mh", alternate = {"maxHealth"})
    protected double maxHealth = 20.0;
    @SerializedName(value = "ms", alternate = {"movementSpeed"})
    protected double movementSpeed = 0.2;

    @Override
    protected void serializer(Entity entity) {
        final AbstractHorse h = (AbstractHorse) entity;

        // TODO: Cleanup
        if (entity.getType() == EntityType.HORSE) {
            Horse horse = (Horse) h;
            horseColor = horse.getColor().name();
            style = horse.getStyle().name();
            saddle = ItemUtils.smallString(horse.getInventory().getSaddle());
            armor = ItemUtils.smallString(horse.getInventory().getArmor());
        } else if (entity.getType() == EntityType.LLAMA) {
            Llama llama = (Llama) h;
            horseColor = llama.getColor().name();
            saddle = ItemUtils.smallString(llama.getInventory().getDecor());
        } else if (entity.getType() == EntityType.MULE || entity.getType() == EntityType.DONKEY
                || entity.getType() == EntityType.ZOMBIE_HORSE || entity.getType() == EntityType.SKELETON_HORSE) {
            // Actually a saddle
            saddle = ItemUtils.smallString(h.getInventory().getItem(0));
        }

        if (entity instanceof ChestedHorse) {
            chest = ((ChestedHorse) entity).isCarryingChest();
        }
        dom = h.getDomestication();
        maxDom = h.getMaxDomestication();
        jump = h.getJumpStrength();
        maxHealth = h.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();

        AttributeInstance attributeInstance = h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (attributeInstance != null) {
            movementSpeed = attributeInstance.getBaseValue();
        }
    }

    @Override
    protected void deserializer(Entity entity) {
        final AbstractHorse h = (AbstractHorse) entity;

        if (entity.getType() == EntityType.HORSE) {
            Horse horse = (Horse) h;
            Horse.Color color = MiscUtils.getEnum(horseColor, Horse.Color.WHITE);
            Horse.Style vstyle = MiscUtils.getEnum(style, Horse.Style.NONE);
            horse.setColor(color);
            horse.setStyle(vstyle);
            horse.getInventory().setSaddle(ItemUtils.itemOf(saddle));
            horse.getInventory().setArmor(ItemUtils.itemOf(armor));
        } else if (entity.getType() == EntityType.LLAMA) {
            Llama llama = (Llama) h;
            Llama.Color color = MiscUtils.getEnum(horseColor, Llama.Color.CREAMY);
            llama.setColor(color);
            llama.getInventory().setDecor(ItemUtils.itemOf(saddle));
        } else if (entity.getType() == EntityType.DONKEY || entity.getType() == EntityType.MULE
                || entity.getType() == EntityType.ZOMBIE_HORSE || entity.getType() == EntityType.SKELETON_HORSE) {
            h.getInventory().setItem(0, ItemUtils.itemOf(saddle));
        }

        if (entity instanceof ChestedHorse) {
            ((ChestedHorse) h).setCarryingChest(Boolean.TRUE.equals(chest));
        }

        maxDom = Math.max(1, maxDom);
        dom = Math.min(Math.max(0, dom), maxDom);
        jump = Math.min(Math.max(0.0, jump), 2.0);

        h.setDomestication(dom);
        h.setMaxDomestication(maxDom);
        h.setJumpStrength(jump);
        h.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);

        AttributeInstance attributeInstance = h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (attributeInstance != null) {
            attributeInstance.setBaseValue(movementSpeed);
        }
    }

    @Override
    protected void niceName(StringBuilder sb, int start) {
        if (horseColor != null) {
            sb.insert(start, MiscUtils.niceName(horseColor)).insert(start + horseColor.length(), ' ');
            start += horseColor.length() + 1;
        }

        if (style != null) {
            sb.insert(start, MiscUtils.niceName(style)).insert(start + style.length(), ' ');
            // start += style.length() + 1;
        }
    }
}
