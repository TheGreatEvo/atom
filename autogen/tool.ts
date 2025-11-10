// generate_items.ts
import { stringify } from "yaml";

// ---------------------- Config ----------------------


const MATERIALS = ["iron", "copper", "bronze", "steel"] as const;
type Material = (typeof MATERIALS)[number];

const ALL_TOOLS = [
    "pickaxe",
    "shovel",
    "hoe",
    "sword",
    "axe",
    "hammer",
    "knife",
    "saw",
] as const;
type ToolType = (typeof ALL_TOOLS)[number];

const CLASSIC_MATERIALS: Material[] = ["iron"];
const CLASSIC_TOOLS: ToolType[] = ["pickaxe", "shovel", "hoe", "sword", "axe"];

// Which material belongs to which tech-age badge
// Update the image keys to match your resource names.
const ageByMaterial: Record<Material, string> = {
    copper: "copper",
    bronze: "bronze",
    iron: "iron",
    steel: "iron",
};

// Molds
type MoldDef = {
    id: string;
    display: string;
};
const MOLDS = [
    { id: "axe_head", display: "Axe Head" },
    { id: "hammer_head", display: "Hammer Head" },
    { id: "ingot", display: "Ingot" },
    { id: "knife_blade", display: "Knife Blade" },
    { id: "pickaxe_head", display: "Pickaxe Head" },
    { id: "saw_blade", display: "Saw Blade" },
    { id: "shovel_head", display: "Shovel Head" },
    { id: "sword_blade", display: "Sword Blade" },
] as const satisfies MoldDef[];
type Mold = (typeof MOLDS)[number];
type MoldId = (typeof MOLDS)[number]["id"];
type MoldVariant = "clay" | "fired" | "wax";

// Textures
const unfiredBaseTexture = "minecraft:item/ceramic/unfired_";
const firedBaseTexture = "minecraft:item/ceramic/fired/";
const waxBaseTexture = "minecraft:item/ceramic/wax_";

function fullToolTexture(material: Material, type: ToolType): string {
    return `minecraft:item/tool/${material}_${type}`;
}
function headTexture(material: Material, type: ToolType): string {
    return `minecraft:item/tool/head/${material}_${type}`;
}
function moldTexture(def: Mold, variant: MoldVariant): string {
    switch (variant) {
        case "wax":
            return waxBaseTexture + def.id;
        case "fired":
            return firedBaseTexture + def.id + "_empty";
        case "clay":
            return unfiredBaseTexture + def.id;
    }
}

// ---------------------- Helpers ----------------------

function shouldGenerateFullTool(material: Material, type: ToolType): boolean {
    if (CLASSIC_MATERIALS.includes(material)) return !CLASSIC_TOOLS.includes(type);
    return true;
}

function toolKey(material: Material, type: ToolType) {
    return `atom:${material}_${type}`;
}
function headKey(material: Material, type: ToolType) {
    return `atom:${material}_${type}_head`;
}
function moldKey(id: MoldId, variant: MoldVariant) {
    return `atom:${variant}_mold_${id}`;
}

function toolBaseMaterial(type: ToolType): string {
    switch (type) {
        case "pickaxe":
            return "iron_pickaxe";
        case "shovel":
            return "iron_shovel";
        case "hoe":
            return "iron_hoe";
        case "sword":
            return "iron_sword";
        case "axe":
            return "iron_axe";
        case "hammer":
            return "iron_pickaxe";
        case "knife":
            return "iron_sword";
        case "saw":
            return "iron_axe";
    }
}

const HEAD_BASE_MATERIAL = "paper";
const CLAY_BASE_MATERIAL = "clay_ball";
const FIRED_OR_WAX_BASE_MATERIAL = "brick";

// L10n builders
function l10nTool(material: Material, type: ToolType) {
    return `<!i><white><lang:item.tool.${material}.${type}.name>`;
}
function l10nToolHead(material: Material, type: ToolType) {
    return `<!i><white><lang:item.tool_head.${material}.${type}.name>`;
}
function l10nMold(variant: MoldVariant, id: MoldId) {
    return `<!i><white><lang:item.mold.${variant}.${id}.name>`;
}

// Lore helpers
function img(key: string) {
    return `<image:atom:${key}>`;
}
function badgeAgeForMaterial(m: Material) {
    return img(`badge_age_${ageByMaterial[m]}`);
}
const BADGE_MATERIAL = img("badge_material");
const BADGE_TOOL = img("badge_tool");
// molds use either material or utility badge depending on the variant in original code
const BADGE_UTILITY = img("badge_utility");

// Shared model block
function simplifiedGeneratedModel(path: string) {
    return {
        template: "default:model/simplified_generated",
        arguments: { path },
    };
}

// ---------------------- Item builders ----------------------

function buildToolHead(material: Material, type: ToolType) {
    const key = headKey(material, type);
    return {
        [key]: {
            material: HEAD_BASE_MATERIAL,
            data: {
                "item-name": l10nToolHead(material, type),
                lore: [
                    "<!i><gray><lang:item.tool_head.common.lore>",
                    "",
                    `<!i><white>${BADGE_MATERIAL} ${badgeAgeForMaterial(material)}`,
                ],
                "remove-components": ["attribute_modifiers"],
            },
            model: simplifiedGeneratedModel(headTexture(material, type)),
        },
    };
}

function buildFullTool(material: Material, type: ToolType) {
    const key = toolKey(material, type);
    return {
        [key]: {
            material: toolBaseMaterial(type),
            data: {
                "item-name": l10nTool(material, type),
                lore: [
                    "<!i><gray><lang:item.tool.common.lore>",
                    "",
                    `<!i><white>${BADGE_TOOL} ${badgeAgeForMaterial(material)}`,
                ],
                "remove-components": ["attribute_modifiers"],
            },
            model: simplifiedGeneratedModel(fullToolTexture(material, type)),
        },
    };
}

function buildMold(def: Mold, variant: MoldVariant) {
    const key = moldKey(def.id, variant);
    const baseMaterial =
        variant === "clay" ? CLAY_BASE_MATERIAL : FIRED_OR_WAX_BASE_MATERIAL;

    const lore: (string | null)[] = [
        "<!i><gray><lang:item.mold.common.lore>",
        variant === "clay" ? "<!i><red><lang:item.mold.unfired.lore>" : null,
        "",
        `<!i><dark_gray><lang:item.mold.${def.id}.lore>`,
        "",
        // Keep original badges: clay uses material+age; fired/wax use utility+age
        variant === "clay"
            ? `<!i><white>${BADGE_MATERIAL} ${img("badge_age_copper")}`
            : `<!i><white>${BADGE_UTILITY} ${img("badge_age_copper")}`,
    ];

    return {
        [key]: {
            material: baseMaterial,
            data: {
                "item-name": l10nMold(variant, def.id),
                lore: lore.filter((l) => l !== null),
            },
            model: simplifiedGeneratedModel(moldTexture(def, variant)),
        },
    };
}

// ---------------------- Generators ----------------------

function generateMolds() {
    const items: Record<string, unknown> = {};
    const list: string[] = [];
    const variants: MoldVariant[] = ["clay", "fired", "wax"];

    for (const def of MOLDS) {
        for (const v of variants) {
            Object.assign(items, buildMold(def, v));
            list.push(moldKey(def.id, v));
        }
    }

    const categories = {
        "atom:molds": {
            name: "<!i><white><lang:category.molds.name></white>",
            hidden: true,
            lore: ["<!i><gray><lang:category.molds.lore>"],
            icon: "atom:clay_mold_shovel_head", // adjust as needed
            list,
        },
    };

    const en: Record<string, string> = {
        "category.molds.name": "Clay and Fired Molds",
        "category.molds.lore": "Reusable molds for shaping tools and parts",
        "item.mold.common.lore": "A mold used in crafting tools and weapons",
        "item.mold.unfired.lore": "Fire this in a kiln to create a mold",
    };

    for (const def of MOLDS) {
        en[`item.mold.wax.${def.id}.name`] = `Wax ${def.display} Mold`;
        en[`item.mold.clay.${def.id}.name`] = `Clay ${def.display} Mold`;
        en[`item.mold.fired.${def.id}.name`] = `Fired ${def.display} Mold`;
        const connecting =
            def.display.toLowerCase().charAt(0) === "a" ? "an" : "a";
        en[`item.mold.${def.id}.lore`] = `Used to make ${connecting} ${def.display}`;
    }

    return { items, categories, lang: { en } };
}

function generateTools() {
    const items: Record<string, unknown> = {};
    const headKeys: string[] = [];
    const toolKeys: string[] = [];

    for (const mat of MATERIALS) {
        for (const t of ALL_TOOLS) {
            Object.assign(items, buildToolHead(mat, t));
            headKeys.push(headKey(mat, t));

            if (shouldGenerateFullTool(mat, t)) {
                Object.assign(items, buildFullTool(mat, t));
                toolKeys.push(toolKey(mat, t));
            }
        }
    }

    const categories = {
        "atom:tools": {
            name: "<!i><white><lang:category.tools.name></white>",
            hidden: true,
            lore: ["<!i><gray><lang:category.tools.lore>"],
            icon: "minecraft:copper_pickaxe", // adjust
            list: toolKeys.sort((a, b) => a.localeCompare(b)),
        },
        "atom:tool_heads": {
            name: "<!i><white><lang:category.tool_heads.name></white>",
            hidden: true,
            lore: ["<!i><gray><lang:category.tool_heads.lore>"],
            icon: "atom:copper_pickaxe_head", // adjust
            list: headKeys.sort((a, b) => a.localeCompare(b)),
        },
    };

    const en: Record<string, string> = {
        "category.tools.name": "Tools",
        "category.tools.lore": "Crafted tools by material",
        "category.tool_heads.name": "Tool Heads",
        "category.tool_heads.lore": "Components used to craft tools",
        "item.tool_head.common.lore": "A shaped head for a tool",
        "item.tool.common.lore": "A durable tool for survival tasks",
    };

    const materialName: Record<Material, string> = {
        iron: "Iron",
        copper: "Copper",
        steel: "Steel",
    };
    const typeName: Record<ToolType, string> = {
        pickaxe: "Pickaxe",
        shovel: "Shovel",
        hoe: "Hoe",
        sword: "Sword",
        axe: "Axe",
        hammer: "Hammer",
        knife: "Knife",
        saw: "Saw",
    };

    for (const m of MATERIALS) {
        for (const t of ALL_TOOLS) {
            en[`item.tool_head.${m}.${t}.name`] = `${materialName[m]} ${typeName[t]} Head`;
            en[`item.tool.${m}.${t}.name`] = `${materialName[m]} ${typeName[t]}`;
        }
    }

    return { items, categories, lang: { en } };
}

// ---------------------- Orchestrate + Write ----------------------

function deepMerge<T extends Record<string, any>>(
    a: T,
    b: T
): T {
    const out: any = Array.isArray(a) ? [...a] : { ...a };
    for (const [k, v] of Object.entries(b)) {
        if (v && typeof v === "object" && !Array.isArray(v)) {
            out[k] = deepMerge(out[k] ?? {}, v);
        } else {
            out[k] = v;
        }
    }
    return out;
}

function asYamlDoc(doc: any) {
    return stringify(doc, { lineWidth: 0 });
}

async function main() {
    const molds = generateMolds();
    const tools = generateTools();

    // Keep separate YAML files (same paths you used). You can also merge to one.
    const moldsYaml = asYamlDoc(molds);
    const toolsYaml = asYamlDoc(tools);

    await Bun.write(
        "../run/plugins/CraftEngine/resources/atom/configuration/auto/molds.yml",
        moldsYaml
    );
    await Bun.write(
        "../run/plugins/CraftEngine/resources/atom/configuration/auto/tools.yml",
        toolsYaml
    );

    console.log(
        "Generated molds.yml (items + category + translations) and tools.yml (heads + tools)"
    );
}

await main();