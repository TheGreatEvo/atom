// scripts/lib/paths.ts
import { writeFileSync, mkdirSync, existsSync } from "fs";
import { dirname } from "path";

const ROOT = "../run/plugins/CraftEngine/resources/atom/configuration/auto";

export const OUT = {
    root: ROOT,
    animalProducts: `${ROOT}/animal_products.yml`,
    foodRecipes: `${ROOT}/food_recipes.yml`,
    molds: `${ROOT}/molds.yml`,
    tools: `${ROOT}/tools.yml`,
    woodRecipes: `${ROOT}/wood_recipes.yml`
} as const;

export function writeTextFile(path: string, text: string) {
    if(!existsSync(dirname(path))) mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path, text);
}