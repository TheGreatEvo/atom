// scripts/generate.ts
import { writeTextFile } from "@lib/paths";
import { logTask } from "@lib/logging";
import type { Generator } from "@gen/types";
import { generateAnimalProducts } from "@gen/animal_products";
import { generateItems } from "@gen/items";
import { generateWoodRecipesFile } from "@gen/wood_recipes";
import { generateVanillaItemLore } from "@gen/vanilla_lore";

const generators: Array<[string, Generator]> = [
    ["Animal products", generateAnimalProducts],
    ["Items (molds + tools)", generateItems],
    ["Wood recipes", generateWoodRecipesFile],
    ["Vanilla item lore", generateVanillaItemLore],
];

async function run() {
    const overall = logTask("Running generators");
    let totalWrites = 0;

    for (const [name, gen] of generators) {
        const t = logTask(name);
        const res = await gen();
        for (const w of res.writes) {
            writeTextFile(w.path, w.content);
            totalWrites++;
        }
        t.done();
    }

    overall.done(`files written: ${totalWrites}`);
}

await run();