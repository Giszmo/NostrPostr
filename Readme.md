# NostrPostr - A nostr client and library for Android


# Overview

This repository consists of multiple modules that will eventually be split into their own
repositories but as long as the basic libraries and tools are in very active development, juggling
multiple repositories isn't worth it yet.

The components are:

## Nostr Library in Kotlin - nostrpostrlib

This is the core all other modules build upon. It provides facilities to connect to nostr relays,
to send and receive events.

## Examples

Here you can find some simple use cases of the library.

## App

Here are the first efforts of developing a full Nostr Social client for Android.

Components to be developed:

- [ ] Profile Editor
- [ ] Profile Picker
- [x] Send and Receive Events
- [ ] Feed
- [ ] Notifications
- [ ] Chat
- [ ] Cache data to local DB

## NostrRelay

This is a full relay implementation. It's not battle tested yet but works fine for the author.

- [x] nip 1
- [x] nip 2
- [x] nip 9
- [x] nip 11
- [x] nip 12
- [x] nip 15
- [x] nip 16


# History

As of the date of creation of this repository there was no Android nostr client. There was no nostr
library for Android neither. The goal is to provide both with a focus on a clean and generally
usable, fully featured library and a client with basic functionality.

# Development

If you want to contribute and aren't a seasoned Android developer yet: Try it. It really is easy.

1. Get [Android Studio](https://developer.android.com/studio/) for free
2. Clone this repository
3. Import it in Android Studio
4. [Create an Android Virtual Device](https://developer.android.com/studio/run/managing-avds)
5. Launch the app (That "play" button top left)

# FAQ

* **What is nostr?** "nostr" stands for "**N**otes and **O**ther **S**tuff **T**ransmitted by
  **R**elays" - a protocol that thanks to its simplicity might actually work to "create a
  censorship-resistant global social network once and for all". Read more about it
  [here](https://github.com/fiatjaf/nostr).
* **Where can I find other tools for nostr?** Check out
  [awesome-nostr](https://github.com/aljazceru/awesome-nostr).
* **Where can I find nostr developers?** On nostr of course but also on Telegram in
  [nostr_protocol](https://t.me/nostr_protocol).