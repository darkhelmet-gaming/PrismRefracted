package network.darkhelmet.prism.actions.entity;

import com.google.gson.annotations.SerializedName;
import network.darkhelmet.prism.actions.data.ItemStackActionData;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

public class MerchantSerializer extends EntitySerializer {
    @SerializedName(value = "rdl", alternate = {"recipeDataList"})
    protected List<RecipeData> recipeDataList = null;

    @Override
    protected void serializer(Entity entity) {
        Merchant merchant = (Merchant) entity;
        recipeDataList = new ArrayList<>();
        for (MerchantRecipe recipe : merchant.getRecipes()) {
            RecipeData recipeData = new RecipeData();
            ItemStack result = recipe.getResult();
            recipeData.result = ItemStackActionData.createData(result, result.getAmount(), result.getDurability(), result.getEnchantments());
            for (ItemStack ingredient : recipe.getIngredients()) {
                recipeData.ingredients.add(ItemStackActionData.createData(ingredient, ingredient.getAmount(), ingredient.getDurability(), ingredient.getEnchantments()));
            }
            recipeData.uses = recipe.getUses();
            recipeData.maxUses = recipe.getMaxUses();
            recipeData.experienceReward = recipe.hasExperienceReward();
            recipeData.villagerExperience = recipe.getVillagerExperience();
            recipeData.priceMultiplier = recipe.getPriceMultiplier();
            recipeDataList.add(recipeData);
        }
    }

    @Override
    protected void deserializer(Entity entity) {
        if (recipeDataList != null) {
            Merchant merchant = (Merchant) entity;
            List<MerchantRecipe> bukkitRecipes = new ArrayList<>();
            for (RecipeData recipeData : recipeDataList) {
                MerchantRecipe bukkit = new MerchantRecipe(recipeData.result.toItem(), recipeData.uses,
                        recipeData.maxUses, recipeData.experienceReward, recipeData.villagerExperience, recipeData.priceMultiplier);
                List<ItemStack> deserializedIngredients = new ArrayList<>();
                for (ItemStackActionData ingredient : recipeData.ingredients) {
                    deserializedIngredients.add(ingredient.toItem());
                }
                bukkit.setIngredients(deserializedIngredients);
                bukkitRecipes.add(bukkit);
            }
            merchant.setRecipes(bukkitRecipes);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class RecipeData {
        public ItemStackActionData result;
        public List<ItemStackActionData> ingredients = new ArrayList<>();
        public int uses;
        public int maxUses;
        public boolean experienceReward;
        public int villagerExperience;
        public float priceMultiplier;
    }
}
