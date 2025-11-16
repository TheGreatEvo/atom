// scripts/generators/wood_recipes.ts
import {asYamlDoc, atom, mc, type AnyRecipe} from "../lib/itemkit";
import {OUT} from "../lib/paths";
import type {Generator} from "./types";

const WOOD_TYPES = [
    "oak",
    "spruce",
    "birch",
    "jungle",
    "acacia",
    "dark_oak",
] as const;
type WoodType = (typeof WOOD_TYPES)[number];

function recipeKey(wood: WoodType) {
    return atom(`saw_${wood}_planks`);
}

function logId(wood: WoodType) {
    // vanilla naming is <wood>_log for these types
    return mc(`${wood}_log`);
}

function planksId(wood: WoodType) {
    return mc(`${wood}_planks`);
}

function buildSawPlankRecipe(wood: WoodType): AnyRecipe {
    return {
        type: "shaped",
        pattern: ["S", "L"],
        ingredients: {
            S: "#" + atom("tool_saw"),
            L: logId(wood),
        },
        result: {
            id: planksId(wood),
            count: 2,
        },
    } as const;
}


function buildType(wood: WoodType, suffix: string, natural: boolean) {
    const key = `minecraft:${wood}_${suffix}`;
    const lore = [
        "",
        natural ? `<!i><white><image:atom:badge_natural> <image:atom:badge_age_foraging>` : `<!i><white><image:atom:badge_material> <image:atom:badge_age_foraging>`,
    ];
    return {
        [key]: {
            "client-bound-data": {lore},
        },
    };
}

function generateWoodRecipes() {
    const recipes: Record<string, AnyRecipe> = {};
    for (const wood of WOOD_TYPES) {
        recipes[recipeKey(wood)] = buildSawPlankRecipe(wood);
    }
    return {recipes};
}

function generateWoodItems() {
    const items: Record<string, unknown> = {};
    for (const wood of WOOD_TYPES) {
        Object.assign(items, buildType(wood, "log", true));
        Object.assign(items, buildType(wood, "planks", false));
        Object.assign(items, buildType(wood, "sapling", true));
    }
    return {items};
}

export const generateWoodRecipesFile: Generator = () => {
    const doc = generateWoodRecipes();
    const yaml = asYamlDoc(doc);

    return {
        writes: [{path: OUT.woodRecipes, content: yaml}, {
            path: OUT.root + "/wood_items.yml", content: asYamlDoc(generateWoodItems())
        }]
    };
};