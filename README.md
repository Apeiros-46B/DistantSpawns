# DistantSpawns

DistantSpawns is a Spigot plugin that prevents players from setting their spawnpoint using beds if they are too far away from the world spawn. To set their spawn far away, players must use respawn anchors, which this plugin also nerfs (they are unstackable, and cannot be further recharged after being spawned from 4 times).

The idea is to encourage players to build their bases closer to spawn, and use anchors to set spawn when exploring far-away high risk structures. I made this plugin for my friends' SMP to prevent people from spreading out too far.

The distance threshold and messages are all configurable, and the options are explained by the comments in `config.yml`.

### Permission groups
`distantspawns.admin` - grants access to the `/distantspawns reload` command
