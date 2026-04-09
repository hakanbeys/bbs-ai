# BBS Mod

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.4-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](https://modrinth.com/mod/bbs-mod)
[![Loader](https://img.shields.io/badge/Loader-Fabric-F7C44F?style=for-the-badge)](https://fabricmc.net/)
[![Version](https://img.shields.io/badge/Version-1.7.7-3B82F6?style=for-the-badge)](#)
[![License](https://img.shields.io/badge/License-MIT-4C6FFF?style=for-the-badge)](./LICENSE.md)
[![Status](https://img.shields.io/badge/Status-Open%20Source%20Handoff-EA5B5B?style=for-the-badge)](#project-status)

BBS Mod is a Minecraft animation and cinematic creation mod for Fabric `1.20.4`. It is designed for building films, scenes, actor performances, replay-driven animation, camera timelines, and advanced in-game content workflows.

This repository is now being published in a more open handoff state. The long-term AI and tooling goals were not fully reached, so the project is being left available for other developers and modders who want to continue it in their own direction.

## Project Status

This codebase is best treated as:

- a usable animation/cinematic framework
- a modding and reverse-engineering playground
- a forkable base for your own version
- a partially finished AI-assisted production toolchain

## Highlights

- Replay-based actor animation
- Film and scene editing
- Camera clips and cinematic timelines
- Model forms and animatable properties
- Content pipeline and in-editor tooling
- Experimental AI-assisted animation workflow

## AI Status

The repository includes a large AI-agent rewrite attempt aimed at better cinematic generation and tool-driven editing.

Current strengths:

- stronger orchestration
- more tool helpers
- improved actor/model/form/camera workflows
- better session memory and diagnostics

Still needs work:

- tool-call reliability with real providers
- malformed provider output handling in practice
- stronger runtime validation
- better cinematic quality under actual use

## Build

```bash
./gradlew build
```

## Contributing

Community forks are the intended path forward.

Recommended approach:

- fork the repository
- maintain your own branch
- test aggressively in runtime
- document behavior changes clearly

## License

MIT License. See [`LICENSE.md`](./LICENSE.md).

## Links

- Modrinth: https://modrinth.com/mod/bbs-mod
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/bbs-mod
