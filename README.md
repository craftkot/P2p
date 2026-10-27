# OpenTogether

Forge 1.20.1. Simple host/join menu for playing Minecraft Java Edition with friends.

**0.4.0-alpha** removes Cloudflare and temporary codes from the mod. Hosts can optionally install **[e4mc 6.2.3 for Forge](https://modrinth.com/mod/e4mc/version/6.2.3-forge)** to expose a LAN world through the e4mc project's free public relay infrastructure, without router port forwarding. Friends do not need e4mc; they join with the public hostname.

OpenTogether does not bundle or impersonate the e4mc relay client. e4mc is a separate, optional mod for remote hosting; without it, vanilla LAN works only on the local network unless the host's TCP port is publicly reachable.

Source directory `cloudflare/` remains only as an unused experimental backend prototype, not used by the current mod.

See [Russian guide](README_RU.md).
