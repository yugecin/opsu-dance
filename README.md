#opsu!dance
[example video](https://www.youtube.com/watch?v=tqZqn7nx8N0)  
  
Originally started as a fork of [opsu!](https://github.com/itdelatrisu/opsu) with cursordance stuff. I made a cursordancing bot in C# for osu!, and by adding it into this clone, it allows me to do even more stuff with it. This way I can also provide this client to other players so they can play with it too, as I will not give my bot to people because I don't want to endorse cheating in any way.

As of 2017 some major changes were made in this fork which changed the inner workings of opsu. This was done in an attempt to get more control over the base system and allowed a.o. changing resolution and skin at runtime without the need to restart the whole system. This fork was pretty even to opsu! before this change, but now there are way more differences.

My goal is to to add cool cursordancing things to this fork, but also make it possible to play the normal way.
  
###Downloads
Click on the releases link (scroll up) to go to the downloadpage with prebuilt jars.

###Building
You can find general (run/build) instructions in the original [opsu! README](README-OPSU.md).
Please note that I am only using maven, gradle scripts are not being updated.

###Credits
opsu! was made by Jeffrey Han ([@itdelatrisu](https://github.com/itdelatrisu)). All game concepts and designs are based on work by osu! developer Dean Herbert. Other opsu! credits can be found [here](CREDITS.md).  
opsu!dance (everything in the src package yugecin.opsudance) was made by me ([@yugecin](https://github.com/yugecin)). Edits were made in the opsu! sources, too.  

###License
**This software is licensed under GNU GPL version 3.**
You can find the full text of the license [here](LICENSE).
