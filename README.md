icestreamer - music streaming, hacker style
=====

## packages needed

* `gstreamer0.10-plugins-ugly` for jPlayer, playing files directly in a browser
* `sox` and `libsox-fmt-all` for song titles

## first build

* `hg clone ssh://hg@bitbucket.org/dmn/icestreamer`
* `cd icestreamer`
* `mvn package`
* `./icestreamer.sh` or `java -Xmx100M -jar target/icestreamer-*-SNAPSHOT.jar --name localhost`
* browse to http://localhost:6680

## add some songs

* `find '/srv/music/' '/mnt/pendrive/music/' -iname '*.mp3' -or -iname '*.ogg' | ./icestreamer.sh`
* browse to http://localhost:6680 and refresh the page

## stream files to mpd

* on the same machine: ```find "`pwd`" -iname '*.mp3' -or -iname '*.ogg'|sort|./icestreamer.sh|while read l;do mpc add "$l";done;mpc play```
* on a different machine ([httpie](https://github.com/jkbrzt/httpie) required) (192.168.1.100 is current machine, 192.168.1.4 is MPD server):

```
export MPD_HOST=192.168.1.4
mpc clear
find "`pwd`" -iname '*.mp3' -or -iname '*.ogg'|sort|http POST '192.168.1.100:6680/'|while read l;do mpc add "$l";done;mpc play
```
