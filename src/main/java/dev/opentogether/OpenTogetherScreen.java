package dev.opentogether;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

final class OpenTogetherScreen extends Screen {
    private final Screen previous;
    private final boolean inviting;
    private EditBox entry;
    private String status="";
    private boolean probeBusy;
    OpenTogetherScreen(Screen previous,boolean inviting) {super(Component.literal("OpenTogether"));this.previous=previous;this.inviting=inviting;}
    @Override protected void init() {
        int x=width/2-120,y=height/2-44;
        entry=new EditBox(font,x,y,240,20,Component.literal(inviting?"Публичный IP:порт":"Код приглашения"));
        entry.setMaxLength(254);entry.setHint(Component.literal(inviting?"example.org:25565":"ABCD-EFGH"));addRenderableWidget(entry);
        addRenderableWidget(Button.builder(Component.literal(inviting?"Создать приглашение":"Подключиться"),b->action())
                .bounds(x,y+28,240,20).build());
        addRenderableWidget(Button.builder(Component.literal("Проверить P2P / UDP"),b->probeNetwork())
                .bounds(x,y+54,240,20).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"),b->minecraft.setScreen(previous))
                .bounds(x,y+80,240,20).build());
        if(!CloudflareApi.configured())status="Не настроен Cloudflare API (-Dopentogether.api=...)";
    }
    private void action() {
        String v=entry.getValue().trim();
        if(v.isEmpty()){status="Введите "+(inviting?"адрес сервера":"код");return;}
        status="Запрос к Cloudflare...";
        var f=inviting?
            CloudflareApi.call("POST","/create","{\"endpoint\":\""+escape(v)+"\"}"):
            CloudflareApi.call("GET","/resolve/"+v.toUpperCase(java.util.Locale.ROOT).replace(" ",""),"");
        f.whenComplete((json,error)->Minecraft.getInstance().execute(()->{
            if(error!=null) {status="Ошибка: "+error.getClass().getSimpleName()+" (см. latest.log)";
                CloudflareApi.LOG.warn("[OpenTogether] invitation failure",error);return;}
            if(inviting){String code=json.get("code").getAsString();minecraft.keyboardHandler.setClipboard(code);
                status="Код "+code+" скопирован. Действует 10 мин.";
                CloudflareApi.LOG.info("[OpenTogether] invite created (code redacted)");
            } else {
                String endpoint=json.get("endpoint").getAsString();
                try {ServerAddress addr=ServerAddress.parseString(endpoint);
                    ServerData data=new ServerData("OpenTogether friend",endpoint,false);
                    ConnectScreen.startConnecting(previous,minecraft,addr,data,false);
                }catch(Exception ex){status="Некорректный адрес";CloudflareApi.LOG.warn("[OpenTogether] connect",ex);}
            }
        }));
    }
    private void probeNetwork() {
        if (probeBusy) return;
        probeBusy=true;
        status="Проверяем STUN/UDP...";
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try { return StunProbe.probe(); }
            catch (java.io.IOException ex) { throw new java.util.concurrent.CompletionException(ex); }
        }).whenComplete((result, failure) -> Minecraft.getInstance().execute(() -> {
            probeBusy=false;
            if (failure!=null) {
                status="STUN недоступен. Проверь VPN и UDP (см. latest.log)";
                CloudflareApi.LOG.warn("[OpenTogether] STUN failed: {}",failure.getClass().getSimpleName());
            } else {
                status="UDP/STUN работает, RTT="+result.rttMs()+" мс. Это не P2P";
                CloudflareApi.LOG.info("[OpenTogether] STUN success rttMs={}",result.rttMs());
            }
        }));
    }
    private static String escape(String s){return s.replace("\\","\\\\").replace("\"","\\\"");}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);super.render(g,mx,my,partial);
        g.drawCenteredString(font,inviting?"Пригласить в мир":"Подключиться к другу",width/2,height/2-56,0xffffff);
        g.drawCenteredString(font,status.length()>62?status.substring(0,62):status,width/2,height/2+74,0xdddddd);
        if(inviting)g.drawCenteredString(font,"Сначала открой мир для LAN и настрой доступный извне порт",width/2,height/2+91,0xffbb66);
    }
    @Override public boolean isPauseScreen(){return false;}
}
