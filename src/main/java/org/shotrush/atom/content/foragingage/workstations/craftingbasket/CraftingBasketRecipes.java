package org.shotrush.atom.content.foragingage.workstations.craftingbasket;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.items.ItemQuality;
import org.shotrush.atom.core.recipe.Recipe;
import org.shotrush.atom.core.recipe.RecipeProvider;
import org.shotrush.atom.core.recipe.ShapelessRecipe;
import org.shotrush.atom.core.recipe.annotation.AutoRegister;
import org.shotrush.atom.core.util.ItemUtil;

import java.util.Arrays;
import java.util.List;

@AutoRegister(priority = 1)
public class CraftingBasketRecipes implements RecipeProvider {
    
    @Override
    public List<Recipe> getRecipes() {
        return Arrays.asList(
            createSpearRecipe(),
            createPressureFlakerRecipe(),
            createKnifeRecipe()
        );
    }
    
    private Recipe createSpearRecipe() {
        ItemStack spear = Atom.getInstance().getItemRegistry().createItem("wood_spear");
        if (spear == null) {
            spear = ItemUtil.createItemWithCustomModel(Material.TRIDENT, "spear");
        }
        
        return new ShapelessRecipe.Builder()
            .id("spear")
            .result(spear)
            .addIngredient("sharpened_flint")
            .addIngredient(Material.STICK)
            .addIngredient(Material.STICK) 
            .addIngredient(Material.VINE)
            .build();
    }
    
    private Recipe createPressureFlakerRecipe() {
        ItemStack pressureFlaker = Atom.getInstance().getItemRegistry().createItem("pressure_flaker");
        if (pressureFlaker == null) {
            return null;
        }
        
        return new ShapelessRecipe.Builder()
            .id("pressure_flaker")
            .result(pressureFlaker)
            .addIngredient("bone")
            .build();
    }
    
    private Recipe createKnifeRecipe() {
        ItemStack knife = Atom.getInstance().getItemRegistry().createItem("knife");
        if (knife == null) {
            return null;
        }
        
        return new ShapelessRecipe.Builder()
            .id("knife")
            .result(knife)
            .addIngredient("sharpened_flint", ItemQuality.HIGH)
            .addIngredient(Material.STICK)
            .anyOf()
                .add("stabilized_leather")
                .add(Material.VINE)
                .done()
            .build();
    }
}
