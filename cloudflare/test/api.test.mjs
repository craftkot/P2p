import test from 'node:test';
import assert from 'node:assert/strict';
import { Rooms } from '../src/index.js';

test('Create and resolve invitation', async () => {
  const records = new Map();
  const worker = new Rooms({storage:{
    async get(k) { return records.get(k); },
    async put(k, v) { records.set(k, v); }
  }});
  const create = await worker.fetch(new Request('https://example.com/create', {
    method:'POST', headers:{'content-type':'application/json'},
    body:JSON.stringify({endpoint:'example.org:25565'})
  }));
  assert.equal(create.status,201);
  const created=await create.json();
  assert.match(created.code,/^[A-Z2-9]{4}-[A-Z2-9]{4}$/);
  const resolve=await worker.fetch(new Request('https://example.com/resolve/'+created.code));
  assert.equal(resolve.status,200);
  assert.equal((await resolve.json()).endpoint,'example.org:25565');
});

test('Reject nonpublic or malformed endpoints', async () => {
  const worker=new Rooms({storage:{get:async()=>undefined,put:async()=>undefined}});
  for(const endpoint of ['localhost:25565','127.0.0.1:25565','host:0','bad address:25565']){
    const r=await worker.fetch(new Request('https://example.com/create',{
      method:'POST',body:JSON.stringify({endpoint})
    }));
    assert.equal(r.status,400,endpoint);
  }
});
