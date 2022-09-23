# Jaudiotagger For Android

This is **a fork of [ijabz/jaudiotagger](https://bitbucket.org/ijabz/jaudiotagger/src/master/)** to
used in Android.

Currently, it is in **Work In Process**

## modifications

- use gradle
- remove test files
- replace Android unsupported Java APIs

# Jaudiotagger

*Jaudiotagger* is a Java API for audio metatagging. Both a common API and format specific APIs are
available, currently supports reading and writing metadata for:

- Mp3
- Flac
- OggVorbis
- Mp4
- Aiff
- Wav
- Wma
- Dsf

<del>
The main project page is http://www.jthink.net/jaudiotagger/ and you can contact the main developer
via email:paultaylor@jthink.net
</del>

## Requirements

*Jaudiotagger* requires Java 1.8

## Include in your Project

Currently, not available.

## Build

Directory structure as follows:

### Under source control

- `src`                  : source code directory


### IDE files

- `.idea/`               : New JetBrains Intellij Project (Config&Metadata)

### License

- `license.txt` : license file

### Local Build details

<del>Build is with [Maven](http://maven.apache.org).</del>
Gradlew now

```shell
 ./gradlew build
```
