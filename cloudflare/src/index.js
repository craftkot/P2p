// OpenTogether invitation broker; no Minecraft traffic passes through Cloudflare.
const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const TTL_MS = 10 * 60 * 1000;
const CREATE_LIMIT = 12; // per hour per source IP, experimental protection
const RESOLVE_LIMIT = 90; // per minute per source IP
function respond(body, status = 200) {
  return Response.json(body, {status, headers: {'cache-control': 'no-store', 'x-content-type-options': 'nosniff'}});
}
function newCode() {
  // Rejection sampling avoids modulo bias.
  const chars = [];
  while (chars.length < 8) {
    const random = new Uint8Array(16);
    crypto.getRandomValues(random);
    for (const b of random) {
      if (b >= 224) continue; // 224 is divisible by 32
      chars.push(ALPHABET[b % 32]);
      if (chars.length === 8) break;
    }
  }
  return chars.slice(0, 4).join('') + '-' + chars.slice(4).join('');
}
function validEndpoint(endpoint) {
  if (typeof endpoint !== 'string' || endpoint.length > 253) return false;
  const match = /^([^:]+):([0-9]{1,5})$/.exec(endpoint);
  if (!match) return false;
  const host = match[1].toLowerCase(), port = Number(match[2]);
  if (!Number.isInteger(port) || port < 1 || port > 65535) return false;

  const labels = host.split('.');
  if (labels.some(label => !/^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/.test(label))) return false;

  const ipv4Parts = labels.map(label => Number(label));
  const looksNumeric = labels.every(label => /^[0-9]+$/.test(label));
  if (looksNumeric) {
    if (labels.length !== 4 || labels.some((s, i) =>
      s.length > 3 || (s.length > 1 && s.startsWith('0')) ||
      ipv4Parts[i] < 0 || ipv4Parts[i] > 255)) return false;

    const [a, b, c] = ipv4Parts;
    // Keep non-routable ranges out of invitation codes, including CGNAT.
    if (a === 0 || a === 10 || a === 127 || a >= 224 ||
        (a === 100 && b >= 64 && b <= 127) ||
        (a === 169 && b === 254) ||
        (a === 172 && b >= 16 && b <= 31) ||
        (a === 192 && (b === 168 || (b === 0 && c === 0))) ||
        (a === 198 && (b === 18 || b === 19)) ||
        (a === 192 && b === 0 && c === 2) ||
        (a === 198 && b === 51 && c === 100) ||
        (a === 203 && b === 0 && c === 113)) return false;
    return true;
  }

  if (labels.length < 2) return false;
  if (['localhost', 'local', 'internal', 'test', 'invalid', 'example'].includes(labels.at(-1)))
    return false;
  return true;
}
export class Rooms {
  constructor(state) { this.storage = state.storage; }
  async limited(ip, scope, count, interval) {
    ip ||= 'unidentified'; // Never bypass throttling when Cloudflare omits the connecting IP.
    const key = 'limit:' + scope + ':' + ip;
    const now = Date.now();
    const old = await this.storage.get(key);
    const next = old && old.until > now ? {until: old.until, count: old.count + 1} : {until: now + interval, count: 1};
    await this.storage.put(key, next, {expirationTtl: Math.max(60, Math.ceil((next.until - now)/1000))});
    return next.count > count;
  }
  async fetch(request) {
    const url = new URL(request.url), parts = url.pathname.split('/').filter(Boolean);
    const action = parts[0], ip = request.headers.get('x-ot-client-ip') || '';
    if (action === 'create' && request.method === 'POST') {
      if (await this.limited(ip, 'create', CREATE_LIMIT, 3600000)) return respond({error: 'RATE_LIMIT'}, 429);
      const raw = await request.text();
      if (raw.length > 4096) return respond({error: 'LARGE_BODY'}, 413);
      let data;
      try {data = JSON.parse(raw);} catch {return respond({error: 'BAD_JSON'}, 400);}
      if (!validEndpoint(data?.endpoint)) return respond({error: 'BAD_ENDPOINT'}, 400);
      for (let tries = 0; tries < 12; tries++) {
        const code = newCode();
        if (await this.storage.get('room:' + code)) continue;
        const expires = Date.now() + TTL_MS;
        await this.storage.put('room:' + code, {endpoint: data.endpoint, expires},
          {expirationTtl: Math.ceil(TTL_MS / 1000)});
        return respond({code, expiresAt: expires}, 201);
      }
      return respond({error: 'RETRY'}, 503);
    }
    if (action === 'resolve' && request.method === 'GET') {
      if (await this.limited(ip, 'resolve', RESOLVE_LIMIT, 60000)) return respond({error: 'RATE_LIMIT'}, 429);
      const code = (parts[1] || '').toUpperCase();
      if (!/^[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$/.test(code) || parts.length !== 2)
        return respond({error: 'BAD_CODE'}, 400);
      const room = await this.storage.get('room:' + code);
      if (!room || room.expires <= Date.now()) return respond({error: 'NOT_FOUND'}, 404);
      return respond({endpoint: room.endpoint, expiresAt: room.expires});
    }
    return respond({error: 'NOT_FOUND'}, 404);
  }
}
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === '/health') return respond({ok: true, relaysGameTraffic: false});
    if (!['/create'].includes(url.pathname) && !url.pathname.startsWith('/resolve/'))
      return respond({error: 'NOT_FOUND'}, 404);
    const id = env.ROOMS.idFromName('rooms-v1');
    const h = new Headers(request.headers);
    h.set('x-ot-client-ip', request.headers.get('cf-connecting-ip') || '');
    return env.ROOMS.get(id).fetch(new Request(request, {headers: h}));
  }
};
