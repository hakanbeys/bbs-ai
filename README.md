# BBS Mod

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](https://modrinth.com/mod/bbs-mod)
[![Loader](https://img.shields.io/badge/Loader-Fabric-F7C44F?style=for-the-badge)](https://fabricmc.net/)
[![Version](https://img.shields.io/badge/Version-1.7.7-3B82F6?style=for-the-badge)](#)
[![License](https://img.shields.io/badge/License-MIT-4C6FFF?style=for-the-badge)](./LICENSE.md)
[![Status](https://img.shields.io/badge/Status-Open%20Source%20Handoff-EA5B5B?style=for-the-badge)](#project-status)

BBS Mod is a Minecraft animation and cinematic creation mod for Fabric `1.20.1`. It is built for films, scenes, replay-based actor animation, camera timelines, model forms, and advanced content workflows inside Minecraft.

This branch is being left available as part of an open-source handoff. The broader AI and toolchain goals were not fully achieved, so the project is now better viewed as a strong base for forks, experiments, and community continuation.

## Project Status

This codebase is best treated as:

- a usable animation/cinematic toolset
- a forkable modding base
- a reference implementation for BBS systems
- a partially finished AI-assisted workflow

## Highlights

- Replay-based actor animation
- Film and scene editing
- Camera clips and timeline control
- Model forms and property animation
- Content pipeline and editor tooling
- Experimental AI-assisted animation workflow

## AI Status

The repository includes a significant AI-agent overhaul attempt aimed at cinematic generation and better tool-driven editing.

Current strengths:

- stronger orchestration
- better helper tools
- improved actor/model/form/camera workflows
- better session memory and diagnostics

Still needs work:

- provider reliability
- malformed tool-call recovery in live usage
- runtime validation
- consistently high cinematic output quality

## Build

```bash
./gradlew build
```

## Contributing

Fork-first development is the recommended path.

If you continue this codebase:

- keep your own branch strategy
- test changes in real runtime conditions
- expect unfinished systems
- document behavior clearly

## License

MIT License. See [`LICENSE.md`](./LICENSE.md).

## Links

- Modrinth: https://modrinth.com/mod/bbs-mod
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/bbs-mod
