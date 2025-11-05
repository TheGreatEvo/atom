package org.shotrush.atom.core.recipe;

import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RecipeManager {
    
    private final Map<String, Recipe> recipes = new HashMap<>();
    
    public void registerRecipe(Recipe recipe) {
        recipes.put(recipe.getId(), recipe);
    }
    
    public void unregisterRecipe(String id) {
        recipes.remove(id);
    }
    
    public Recipe getRecipe(String id) {
        return recipes.get(id);
    }
    
    public ItemStack findMatch(List<ItemStack> items) {
        for (Recipe recipe : recipes.values()) {
            if (recipe.matches(items)) {
                return recipe.getResult();
            }
        }
        return null;
    }
    
    public List<Recipe> getAllRecipes() {
        return new ArrayList<>(recipes.values());
    }
    
    public List<Recipe> getRecipesByType(Recipe.RecipeType type) {
        List<Recipe> result = new ArrayList<>();
        for (Recipe recipe : recipes.values()) {
            if (recipe.getType() == type) {
                result.add(recipe);
            }
        }
        return result;
    }
    
    public void clear() {
        recipes.clear();
    }
}
