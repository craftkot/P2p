// OpenTogether code broker. It does NOT transport Minecraft packets.
const ALPHABET='ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const TTL=600000;
function reply(o,s=200){return Response.json(o,{status:s,headers:{'cache-control':'no-store'}})}
function code(){const b=new Uint8Array(8);crypto.getRandomValues(b);let s='';for(const n of b)s+=ALPHABET[n%ALPHABET.length];return s.slice(0,4)+'-'+s.slice(4)}
function valid(e){if(typeof e!=='string'||e.length>254)return false;const i=e.lastIndexOf(':');if(i<1)return false;const host=e.slice(0,i),p=Number(e.slice(i+1));return /^[a-zA-Z0-9.-]+$/.test(host)&&Number.isInteger(p)&&p>0&&p<65536}
export class Rooms {
  constructor(state){this.storage=state.storage}
  async fetch(req){
    const u=new URL(req.url),s=u.pathname.split('/').filter(Boolean);
    if(s[0]==='create'&&req.method==='POST'){
      const raw=await req.text();if(raw.length>4096)return reply({error:'LARGE_BODY'},413);
      let body;try{body=JSON.parse(raw)}catch{return reply({error:'BAD_JSON'},400)}
      if(!valid(body.endpoint))return reply({error:'BAD_ENDPOINT'},400);
      let c;for(let i=0;i<8;i++){const v=code();if(!await this.storage.get(v)){c=v;break}}
      if(!c)return reply({error:'COLLISION'},503);
      await this.storage.put(c,{endpoint:body.endpoint,expires:Date.now()+TTL});
      return reply({code:c,expiresAt:Date.now()+TTL},201);
    }
    if(s[0]==='resolve'&&req.method==='GET'){
      const c=(s[1]||'').toUpperCase();if(!/^[A-Z2-9]{4}-[A-Z2-9]{4}$/.test(c))return reply({error:'BAD_CODE'},400);
      const r=await this.storage.get(c);if(!r||Date.now()>r.expires)return reply({error:'NOT_FOUND'},404);
      return reply({endpoint:r.endpoint,expiresAt:r.expires});
    }
    return reply({error:'NOT_FOUND'},404)
  }
}
export default {async fetch(req,env){const u=new URL(req.url);if(u.pathname==='/health')return reply({ok:true,relaysGameTraffic:false});const id=env.ROOMS.idFromName('rooms-v1');return env.ROOMS.get(id).fetch(req)}};
