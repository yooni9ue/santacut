import { writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import openapiTS, { astToString } from 'openapi-typescript';

const schemaUrl = process.env.SANTA_API_SPEC_URL ?? 'http://localhost:8080/v3/api-docs';
const outputPath = fileURLToPath(new URL('../src/shared/api/schema.d.ts', import.meta.url));
const ast = await openapiTS(new URL(schemaUrl), { propertiesRequiredByDefault: true });

await writeFile(outputPath, astToString(ast), 'utf8');
process.stdout.write(`Generated API types from ${schemaUrl}\n`);
