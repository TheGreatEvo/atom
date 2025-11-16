// scripts/lib/logging.ts
export function logTask(title: string) {
    const start = Date.now();
    console.log(`[gen] ${title}...`);
    return {
        done(extra?: string) {
            const ms = Date.now() - start;
            console.log(`[gen] ✅ ${title} (${ms}ms)${extra ? " " + extra : ""}`);
        }
    };
}