// scripts/lib/itemkit.ts
import { stringify } from "yaml";

// ------------- Namespaced keys -------------
export function nsKey(ns: string, name: string) {
    return `${ns}:${name}`;
}
export function atom(name: string) {
    return nsKey("atom", name);
}
export function mc(name: string) {
    return nsKey("minecraft", name);
}
export function isNamespaced(id: string) {
    return /^[a-z0-9_\-]+:[a-z0-9_\-/.]+$/.test(id);
}

// ------------- YAML helpers -------------
export function asYamlDoc(doc: unknown): string {
    return stringify(doc, { lineWidth: 0 });
}

// ------------- Merge helpers -------------
export function deepMerge<T extends Record<string, any>>(a: T, b: T): T {
    const out: any = Array.isArray(a) ? [...(a as any)] : { ...a };
    for (const [k, v] of Object.entries(b)) {
        if (v && typeof v === "object" && !Array.isArray(v)) {
            out[k] = deepMerge(out[k] ?? {}, v as any);
        } else {
            out[k] = v;
        }
    }
    return out;
}

// ------------- Badges & l10n helpers -------------
const img = (key: string) => `<image:atom:${key}>`;
export const badge = (key: string) => img(`badge_${key}`);
export const ageBadge = (age: string) => img(`badge_age_${age}`);

export function l10nKey(scope: string, ...parts: string[]) {
    return `<!i><white><l10n:${[scope, ...parts].join(".")}.name>`;
}
export function langKey(scope: string, ...parts: string[]) {
    return `<!i><white><lang:${[scope, ...parts].join(".")}.name>`;
}
export function loreLine(text: string) {
    return `<!i><gray>${text}`;
}

// Lore builder for badges
export type BadgeKey =
    | "natural"
    | "material"
    | "utility"
    | "tool"
    | "food"
    | "weapon"
    | "armor"
    | "alchemy"
    | "magic";

export function loreBadges(labels: { badges?: BadgeKey[]; age?: string }) {
    const parts: string[] = [];
    if (labels.badges && labels.badges.length > 0) {
        parts.push(...labels.badges.map((b) => badge(b)));
    }
    if (labels.age) {
        parts.push(ageBadge(labels.age));
    }
    // Put a blank line above the badge row for visual separation, like your wood code
    return ["", `<!i><white>${parts.join(" ")}`];
}

// ------------- Model types -------------
export type SimplifiedGeneratedModel = {
    template: "default:model/simplified_generated";
    arguments: { path: string };
};
export type MinecraftModel = {
    type: "minecraft:model";
    path: string;
    generation?: Record<string, unknown>;
    tints?: unknown[];
};
export type ModelArg = string | SimplifiedGeneratedModel | MinecraftModel;
export function simplifiedGeneratedModel(
    path: string,
): SimplifiedGeneratedModel {
    return {
        template: "default:model/simplified_generated",
        arguments: { path },
    };
}
export function normalizeModel(
    model: ModelArg,
): SimplifiedGeneratedModel | MinecraftModel {
    return typeof model === "string" ? simplifiedGeneratedModel(model) : model;
}

// ------------- Item & Category types -------------
export type ItemNode = {
    material?: string;
    model?: SimplifiedGeneratedModel | MinecraftModel;
    "client-bound-data"?: { lore: string[] };
    settings?: {
        tags?: string[];
        [key: string]: unknown;
    };
    data?: {
        "item-name"?: string;
        "remove-components"?: string[];
        [key: string]: unknown;
    };
};

export type ItemEntries = Record<string, ItemNode>;

export type BuildItemOptions = {
    removeComponents?: string[];
    tags?: string[];
    additionalSettings?: Record<string, unknown>;
    additionalData?: Record<string, unknown>;
};

export type CategoryNode = {
    name: string;
    hidden: boolean;
    lore: string[];
    icon: string;
    list: string[];
    priority: number;
};
export type CategoryEntries = Record<string, CategoryNode>;

// ------------- Recipe types -------------
export type RecipeIngredientMap = Record<string, string>;
export type ShapedRecipe = {
    type: "shaped";
    pattern: string[];
    ingredients: RecipeIngredientMap;
    result: { id: string; count: number };
};
export type ShapelessRecipe = {
    type: "shapeless";
    ingredients: RecipeIngredientMap;
    result: { id: string; count: number };
};
export type CampfireRecipe = {
    type: "campfire_cooking";
    category?: string;
    time: number;
    ingredient: string;
    result: { id: string; count: number };
};
export type AnyRecipe = ShapedRecipe | ShapelessRecipe | CampfireRecipe;
export type RecipeEntries = Record<string, AnyRecipe>;

// ------------- Tag helpers -------------
export function ensureTags(arr?: string[]): string[] | undefined {
    if (!arr || arr.length === 0) return undefined;
    return arr.map((t) => (isNamespaced(t) ? t : atom(t)));
}

// ------------- Builders -------------
export function buildItemEntry(
    key: string,
    baseMaterial: string,
    displayName: string,
    lore: string[],
    model: ModelArg,
    options?: BuildItemOptions,
): ItemEntries {
    const remove = options?.removeComponents;
    const tags = ensureTags(options?.tags) ?? [];
    const additionalSettings = options?.additionalSettings ?? {};
    const additionalData = options?.additionalData ?? {};
    const modelNode = normalizeModel(model);

    const dataPart: Record<string, unknown> = {
        "item-name": displayName,
        ...(remove ? { "remove-components": remove } : {}),
        ...additionalData,
    };

    const settings = { ...additionalSettings };
    if (tags.length > 0) settings.tags = tags;

    const node: ItemNode = {
        material: baseMaterial,
        model: modelNode,
        ...(lore.length > 0 ? { "client-bound-data": { lore } } : {}),
        ...(Object.keys(settings).length > 0 ? { settings } : {}),
        data: Object.keys(dataPart).length ? (dataPart as ItemNode["data"]) : undefined,
    };

    return { [key]: node };
}

export function buildLoreOnlyItem(
    key: string,
    lore: string[],
    opts?: { tags?: string[] },
): ItemEntries {
    return {
        [key]: {
            ...(lore.length > 0 ? { "client-bound-data": { lore } } : {}),
            ...(opts?.tags ? { settings: { tags: ensureTags(opts.tags) } } : {}),
        },
    };
}

export function buildCategory(
    key: string,
    icon: string,
    list: string[],
    opts?: {
        hidden?: boolean;
        priority?: number;
        nameKey?: string;
        loreKey?: string;
    },
): CategoryEntries {
    const hidden = opts?.hidden ?? false;
    const priority = opts?.priority ?? 0;
    const nameKey = opts?.nameKey ?? `category.${key}.name`;
    const loreKey = opts?.loreKey ?? `category.${key}.lore`;

    return {
        [atom(key)]: {
            name: `<!i><white><l10n:${nameKey}></white>`,
            hidden,
            lore: [`<!i><gray><l10n:${loreKey}>`],
            icon,
            list,
            priority,
        },
    };
}