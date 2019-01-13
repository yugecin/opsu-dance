# opsu!dance

**Table of contents**

* [What](#what) - [Why](#why) - [Downloads](#downloads) - [Running](#running) - [Building a JAR](#building-a-jar) - [Credits](#credits) - [License](#license)

What
----
Fork of [opsu!](https://github.com/itdelatrisu/opsu), which is a clone of the [osu!](https://osu.ppy.sh/) rythm game.

* [example video: Cursor Dance | MOMOIRO CLOVER Z - SANTA SAN](https://youtu.be/tqZqn7nx8N0)
* [example video: osu! 50 top replays | Getty vs. DJ DiA - Fox4-Raize- [Extreme]](https://youtu.be/T2AiGn2xOQo)

As of 2017 some major changes were made in this fork which changed the inner workings of opsu. This was done in an attempt to get more control over the base system and allowed a.o. changing resolution and skin at runtime without the need to restart the whole system. This fork was pretty even to opsu! before this change, but now there are way more differences.

Why
---
I made a cursordancing bot in C# for osu!, and by adding it into this clone, it allows me to do even more stuff with it. This way I can also provide this client to other players so they can play with it too, as I will not give my bot to people because I don't want to endorse cheating in any way.

My goal is to to add cool cursordancing things to this fork, but also make it possible to play the normal way. In the meantime I'm also adding various improvements to opsu! while I make a mess here, like the option menu, default back button, slider stuff, ...

Downloads
---------
You can find prebuilt jars on [the releases page](https://github.com/yugecin/opsu-dance/releases).

Running
-------

If you don't need to edit the source, just download a jar from [the releases page](https://github.com/yugecin/opsu-dance/releases).

Using an IDE is recommended because it is usually faster than the other options and provides debugging.

### Using your favorite IDE
You should know how to do this. It's recommended to use a working directory like `out` to not pollute the project directory with config/db files.

### Using apache maven
`mvn compile`

### Using apache ant

Resolve dependencies first by doing `ant mvnresolve` or `mvn initialize`

Then do `ant run`

#### Running tests

Requires `junit.jar` (I use junit 4) in your `ANT_HOME/lib`

`ant test`

Building a JAR
--------------

Using ant is recommended. Ant is used since release 0.5.0

### Using apache maven
`mvn package -Djar`, find it in the `target` folder.

### Using apache ant
`ant jar`, find it in the `bin` folder


Credits
-------
opsu! was made by Jeffrey Han ([@itdelatrisu](https://github.com/itdelatrisu)). All game concepts and designs are based on work by osu! developer Dean Herbert. Other opsu! credits can be found [here](CREDITS.md).

opsu!dance (everything in the src package yugecin.opsudance) was made by me ([@yugecin](https://github.com/yugecin)). Lots of edits were made in the opsu! sources, too.

License
-------
**This software is licensed under GNU GPL version 3.**
You can find the full text of the license in [the LICENSE file](LICENSE).

NB: Some source files in this repository are authored by others who are not associated with this project. See the copyright headers in those files for more details, if applicable. Those files may be modified, with modifications either annotated in source and/or visible in revision information.
