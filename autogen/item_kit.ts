// scripts/lib/itemkit.ts
import { stringify } from "yaml";

// ------------ Generic key helpers ------------
export function nsKey(ns: string, name: string) {
    return `${ns}:${name}`;
}
export function atom(name: string) {
    return nsKey("atom", name);
}
export function mc(name: string) {
    return nsKey("minecraft", name);
}

// ------------ YAML helpers ------------
export function asYamlDoc(doc: any) {
    return stringify(doc, { lineWidth: 0 });
}

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

// ------------ Badges & l10n helpers ------------
const img = (key: string) => `<image:atom:${key}>`;
export const badge = (key: string) => img(`badge_${key}`);
export const ageBadge = (age: string) => img(`badge_age_${age}`);

export function l10nKey(scope: string, ...parts: string[]) {
    return `<!i><white><l10n:${[scope, ...parts].join(".")}.name>`;
}

export function loreLine(text: string) {
    return `<!i><gray>${text}`;
}

// ------------ Models ------------
export function simplifiedGeneratedModel(path: string) {
    return {
        template: "default:model/simplified_generated",
        arguments: { path },
    };
}

export type ModelArg =
    | string
    | {
    type?: string;
    path?: string;
    generation?: Record<string, unknown>;
    template?: string;
    arguments?: Record<string, unknown>;
    tints?: unknown[];
};

// ------------ Items & Categories ------------
export function buildItemEntry(
    key: string,
    baseMaterial: string,
    displayName: string,
    lore: string[],
    model: ModelArg,
    options?: {
        removeComponents?: string[];
        tags?: string[];
        additionalSettings?: Record<string, unknown>;
        additionalData?: Record<string, unknown>;
    }
) {
    const remove = options?.removeComponents;
    const tags = options?.tags ?? [];
    const additionalSettings = options?.additionalSettings ?? {};
    const additionalData = options?.additionalData ?? {};
    const modelNode =
        typeof model === "string" ? simplifiedGeneratedModel(model) : model;

    const dataPart: Record<string, unknown> = {
        "item-name": displayName,
        ...(remove ? { "remove-components": remove } : {}),
        ...additionalData,
    };

    const settings = {...additionalSettings};
    if (tags.length > 0) settings.tags = tags;

    const node: Record<string, any> = {
        material: baseMaterial,
        model: modelNode,
        ...(Object.keys(settings).length > 0 ? {settings} : {}),
    };
    if(lore.length > 0) node['client-bound-data'] = { lore };

    node.data = { ...dataPart };

    return {
        [key]: node,
    };
}

export function buildCategory(
    key: string,
    icon: string,
    list: string[],
    opts?: { hidden?: boolean; priority?: number; nameKey?: string; loreKey?: string }
) {
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
    } as const;
}