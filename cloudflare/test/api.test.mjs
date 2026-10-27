import test from 'node:test';
import assert from 'node:assert/strict';
import {Rooms} from '../src/index.js';

function mock() {
  const records = new Map();
  const storage = {
    async get(key) {return records.get(key);},
    async put(key, value) {records.set(key, value);}
  };
  return {records, rooms: new Rooms({storage})};
}
const create = (endpoint, headers={}) => new Request('https://example.com/create',
  {method:'POST', headers, body:JSON.stringify({endpoint})});
const resolve = (code, headers={}) => new Request('https://example.com/resolve/'+code,{headers});

test('create, resolve, and expire an invitation', async () => {
  const {records, rooms} = mock();
  const result = await rooms.fetch(create('example.org:25565'));
  assert.equal(result.status,201);
  const room = await result.json();
  assert.match(room.code,/^[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$/);
  assert.equal((await (await rooms.fetch(resolve(room.code))).json()).endpoint,'example.org:25565');
  records.get('room:'+room.code).expires=Date.now()-1000;
  assert.equal((await rooms.fetch(resolve(room.code))).status,404);
});

test('reject invalid or internal endpoints', async () => {
  const {rooms} = mock();
  for(const endpoint of ['localhost:25565','127.0.0.1:25565','192.168.1.2:25565',
    '10.0.0.2:25565','172.20.1.2:25565','0.0.0.0:25565',
    'host:0','bad address:25565','host:65536']) {
    assert.equal((await rooms.fetch(create(endpoint))).status,400,endpoint);
  }
});

test('limit repeated creations from one source', async () => {
  const {rooms} = mock();
  const h={'x-ot-client-ip':'198.51.100.3'};
  for(let i=0;i<12;i++) assert.equal((await rooms.fetch(create('example.org:25565',h))).status,201);
  assert.equal((await rooms.fetch(create('example.org:25565',h))).status,429);
});

test('do not accept malformed codes', async () => {
  const {rooms}=mock();
  for(const c of ['abc','IIII-OOOO','1234-1234-EXTRA']) {
    assert.equal((await rooms.fetch(resolve(c))).status,400);
  }
});


test('reject CGNAT, reserved IPv4, invalid host labels and unsafe suffixes', async () => {
  const {rooms} = mock();
  for (const endpoint of [
    '100.64.1.3:25565', '100.127.255.254:25565',
    '169.254.0.1:25565', '203.0.113.7:25565',
    '198.18.1.1:25565', '224.0.0.1:25565',
    '999.0.0.1:25565', '192.168.1.5:25565',
    'my-computer.local:25565', 'singlelabel:25565',
    'a.-bad.net:25565', '-bad.net:25565', 'good..net:25565'
  ]) {
    assert.equal((await rooms.fetch(create(endpoint))).status, 400, endpoint);
  }
  assert.equal((await rooms.fetch(create('8.8.8.8:25565'))).status, 201);
  assert.equal((await rooms.fetch(create('friends.example.net:25565'))).status, 201);
});

test('unknown IP cannot avoid rate limiting', async () => {
  const {rooms} = mock();
  for (let i = 0; i < 12; i++)
    assert.equal((await rooms.fetch(create('friends.example.net:25565'))).status, 201);
  assert.equal((await rooms.fetch(create('friends.example.net:25565'))).status, 429);
});

test('unsupported routes or methods never create a code', async () => {
  const {rooms} = mock();
  assert.equal((await rooms.fetch(new Request('https://example.com/create'))).status,404);
  assert.equal((await rooms.fetch(new Request('https://example.com/resolve/ABC'))).status,400);
  assert.equal((await rooms.fetch(new Request('https://example.com/resolve/ABCD-EFGH/other'))).status,400);
});
