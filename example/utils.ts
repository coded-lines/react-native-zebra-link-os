// Extract placeholders found inside ^FD ... ^FS blocks
export function extractVars(template: string): string[] {
  const set = new Set<string>();
  for (const m of template.matchAll(/\^FD([\s\S]*?)\^FS/g)) {
    const token = m[1].trim();
    if (/^[a-z][a-z0-9_]*$/.test(token)) set.add(token);
  }
  return Array.from(set);
}

// ZPL-safe escaping when ^FH\ is used (your template uses ^FH\)
export function zplEscapeForFH(value: string): string {
  return String(value).replace(/\\/g, "\\\\").replace(/\^/g, "\\^");
}

/**
 * Replace the content of ^FD ... ^FS when it exactly matches a key in `values`.
 * This avoids any lookbehind and works reliably with ZPL.
 */
export function fillPrn(
  template: string,
  values: Record<string, string>,
  opts?: { assumeFH?: boolean }, // default true since your lines include ^FH\
): string {
  const assumeFH = opts?.assumeFH ?? true;

  return template.replace(/\^FD([\s\S]*?)\^FS/g, (full, inner) => {
    const key = String(inner).trim();
    if (Object.prototype.hasOwnProperty.call(values, key)) {
      const val = assumeFH ? zplEscapeForFH(values[key]) : values[key];
      return `^FD${val}^FS`;
    }
    return full; // leave untouched if not a placeholder
  });
}
