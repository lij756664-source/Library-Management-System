// Local-only QA credentials. Never print the returned values.
const fs = require('fs');
const path = require('path');
const configPath = path.join(__dirname, '../springboot_back/src/main/resources/application-local.yml');
function credential(key) {
  const envKey = { username: 'DB_USERNAME', password: 'DB_PASSWORD' }[key];
  if (!envKey) throw new Error('Unsupported credential field');
  if (process.env[envKey] !== undefined) return process.env[envKey];
  if (!fs.existsSync(configPath)) throw new Error('Create application-local.yml from the example before running QA scripts');
  const match = fs.readFileSync(configPath, 'utf8').match(new RegExp('^    ' + key + ': (.*)$', 'm'));
  if (!match) throw new Error('Missing local database configuration: ' + key);
  const value = match[1].trim();
  if (value.startsWith('"')) return JSON.parse(value);
  if (value.startsWith("'")) return value.slice(1, -1).replace(/''/g, "'");
  return value;
}
module.exports = { credential };
