# OpenTogether 0.5.0-alpha — experimental one-JAR build

Minecraft 1.20.1 Forge 47.x. **Only one installation file**: `opentogether-forge-1.20.1-0.5.0-alpha-all.jar`.

This build **bundles e4mc 6.2.3 unchanged** through ForgeGradle's official Jar-in-Jar mechanism. e4mc remains an identifiable upstream mod nested inside the JAR, rather than new networking code written by OpenTogether. e4mc uses public relay service operated by its upstream team, subject to availability and service policies. No Cloudflare settings, Steam, VPN, or router port forwarding are required for typical sessions.

- Source of upstream networking engine: https://github.com/vgskye/e4mc-minecraft-architectury
- e4mc license: MIT, copyright 2024 Skye, included in `licenses/e4mc-MIT.txt`
- Forge Jar-in-Jar docs: https://docs.minecraftforge.net/en/fg-6.x/dependencies/jarinjar/
- **Experimental:** Forge may or may not recognize nested mod components across all installations; test loading in Minecraft before treating this build as verified.

Install **only the `-all.jar`** file in `mods`; remove the old OpenTogether JAR and any separately installed e4mc JAR first.

Enter a single-player world, click Esc → Invite friend → Open to LAN. Once e4mc reports a temporary public hostname in chat, share the hostname with your friends. Friends can connect using an ordinary Minecraft client with a matching version and matching gameplay mods.

See `README_RU.md` for the general guide.
