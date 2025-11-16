// scripts/generators/vanilla_item_lore.ts
import {
    asYamlDoc,
    mc,
    buildLoreOnlyItem,
    loreBadges,
    type ItemEntries,
} from "@lib/itemkit";
import {OUT} from "@lib/paths";
import type {Generator} from "./types";

// Domain: common vanilla groups you can expand freely.
const AGES = {
    foraging: "foraging",
    copper: "copper",
    bronze: "bronze",
    iron: "iron",
} as const;

type AgeKey = keyof typeof AGES;
type BadgeKey = "natural" | "material" | "utility" | "food" | "tool" | "weapon" | "armor";

// A declarative registry. Each entry defines:
// - id: either plain "minecraft:..." or just "minecraft_name" (we normalize)
// - optional tags (for querying later)
// - badges and age, which will render the standardized lore line
type LoreDef = {
    id: string; // can be "minecraft:oak_log" or "oak_log" (we normalize to mc())
    badges?: Array<BadgeKey>;
    age?: AgeKey | string; // allow custom ages too
    tags?: string[];
};

// Helper to normalize vanilla ids (accepts "oak_log" or "minecraft:oak_log")
function normalizeVanillaId(id: string): string {
    if (id.includes(":")) return id; // already namespaced
    return mc(id);
}

// Example registry starter — expand as desired
const VANILLA_LORE: LoreDef[] = [
    // Natural materials
    {id: "vine", badges: ["natural"], age: AGES.foraging},
    {id: "dirt", badges: ["natural"], age: AGES.foraging},
    {id: "coarse_dirt", badges: ["natural"], age: AGES.foraging},
    {id: "podzol", badges: ["natural"], age: AGES.foraging},
    {id: "grass_block", badges: ["natural"], age: AGES.foraging},
    {id: "mud", badges: ["natural"], age: AGES.foraging},
    {id: "packed_mud", badges: ["natural"], age: AGES.foraging},
    {id: "stone", badges: ["natural"], age: AGES.foraging},
    {id: "calcite", badges: ["natural"], age: AGES.foraging},
    {id: "dripstone_block", badges: ["natural"], age: AGES.foraging},
    {id: "pointed_dripstone", badges: ["natural"], age: AGES.foraging},
    {id: "deepslate", badges: ["natural"], age: AGES.foraging},
    {id: "snow_block", badges: ["natural"], age: AGES.foraging},
    {id: "snow", badges: ["natural"], age: AGES.foraging},
    {id: "ice", badges: ["natural"], age: AGES.foraging},
    {id: "obsidian", badges: ["natural"], age: AGES.foraging},
    {id: "andesite", badges: ["natural"], age: AGES.foraging},
    {id: "granite", badges: ["natural"], age: AGES.foraging},
    {id: "tuff", badges: ["natural"], age: AGES.foraging},
    {id: "sand", badges: ["natural"], age: AGES.foraging},
    {id: "red_sand", badges: ["natural"], age: AGES.foraging},
    {id: "sandstone", badges: ["natural"], age: AGES.foraging},
    {id: "red_sandstone", badges: ["natural"], age: AGES.foraging},
    {id: "gravel", badges: ["natural"], age: AGES.foraging},
    {id: "terracotta", badges: ["natural"], age: AGES.foraging},
    {id: "mossy_cobblestone", badges: ["natural"], age: AGES.foraging},
    {id: "moss_block", badges: ["natural"], age: AGES.foraging},
    {id: "moss_carpet", badges: ["natural"], age: AGES.foraging},
    {id: "fern", badges: ["natural"], age: AGES.foraging},
    {id: "rose_bush", badges: ["natural"], age: AGES.foraging},
    {id: "poppy", badges: ["natural"], age: AGES.foraging},
    {id: "dandelion", badges: ["natural"], age: AGES.foraging},
    {id: "seeds", badges: ["natural"], age: AGES.foraging},
    {id: "wheat", badges: ["natural"], age: AGES.foraging},
    {id: "carrot", badges: ["natural"], age: AGES.foraging},
    {id: "potato", badges: ["natural"], age: AGES.foraging},

    // Ores
    {id: "raw_coal", badges: ["natural"], age: AGES.foraging},
    {id: "coal_ore", badges: ["natural"], age: AGES.foraging},
    {id: "deepslate_coal_ore", badges: ["natural"], age: AGES.foraging},
    {id: "raw_copper", badges: ["natural"], age: AGES.copper},
    {id: "copper_ore", badges: ["natural"], age: AGES.copper},
    {id: "deepslate_copper_ore", badges: ["natural"], age: AGES.copper},
    {id: "raw_iron", badges: ["natural"], age: AGES.iron},
    {id: "iron_ore", badges: ["natural"], age: AGES.iron},
    {id: "deepslate_iron_ore", badges: ["natural"], age: AGES.iron},

    // Processed materials
    {id: "stick", badges: ["material"], age: AGES.foraging},
    {id: "iron_ingot", badges: ["material"], age: AGES.iron},
    {id: "copper_ingot", badges: ["material"], age: AGES.copper},
    {id: "iron_nugget", badges: ["material"], age: AGES.copper},
    {id: "copper_nugget", badges: ["material"], age: AGES.copper},
    {id: "iron_block", badges: ["material"], age: AGES.copper},

    // Food
    {id: "apple", badges: ["food", "natural"], age: AGES.foraging},
    {id: "bread", badges: ["food"], age: AGES.foraging},

    // Utility
    {id: "bucket", badges: ["utility"], age: AGES.iron},
    {id: "shears", badges: ["utility"], age: AGES.iron},
];

function buildLoreFor(def: LoreDef): ItemEntries {
    const key = normalizeVanillaId(def.id);
    const lore = loreBadges({badges: def.badges, age: def.age});
    return buildLoreOnlyItem(key, lore, {tags: def.tags});
}

function generateVanillaLoreItems() {
    const items: Record<string, unknown> = {};
    for (const def of VANILLA_LORE) {
        Object.assign(items, buildLoreFor(def));
    }
    return {items};
}

export const generateVanillaItemLore: Generator = () => {
    const doc = generateVanillaLoreItems();
    const yaml = asYamlDoc(doc);

    return {
        // You can change filename; keeping it in /auto for consistency
        writes: [
            {
                path: `${OUT.root}/vanilla_item_lore.yml`,
                content: yaml,
            },
        ],
    };
};