// scripts/generators/types.ts
export type GeneratorResult = {
    writes: Array<{ path: string; content: string }>;
};
export type Generator = () => Promise<GeneratorResult> | GeneratorResult;