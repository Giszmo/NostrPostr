# NostrPostr - A nostr client and library for Android


# Overview

This repository consists of multiple modules that will eventually be split into their own
repositories but as long as the basic libraries and tools are in very active development, juggling
multiple repositories isn't worth it yet.

## NIP support

Library, Relay and/or App implement the following NIPs. Not all NIPs apply to all parts and some
NIPs might not be implemented fully.

- [x] NIP-01: Basic protocol flow description
- [x] NIP-02: Contact List and Petnames
- [ ] NIP-03: OpenTimestamps Attestations for Events
- [x] NIP-04: Encrypted Direct Message
- [ ] NIP-05: Mapping Nostr keys to DNS-based internet identifiers
- [ ] NIP-06: Basic key derivation from mnemonic seed phrase
- [ ] NIP-07: window.nostr capability for web browsers
- [x] NIP-09: Event Deletion
- [x] NIP-11: Relay Information Document
- [x] NIP-12: Generic Tag Queries
- [ ] NIP-13: Proof of Work
- [ ] NIP-14: Subject tag in text events.
- [x] NIP-15: End of Stored Events Notice
- [x] NIP-16: Event Treatment
- [ ] NIP-20: Command Results
- [ ] NIP-22: Event created_at Limits
- [ ] NIP-25: Reactions
- [ ] NIP-26: Delegated Event Signing
- [ ] NIP-28: Public Chat
- [ ] NIP-35: User Discovery
- [ ] NIP-36: Sensitive Content
- [ ] NIP-40: Expiration Timestamp

## Nostr Library in Kotlin - nostrpostrlib

This is the core all other modules build upon. It provides facilities to connect to nostr relays,
to send and receive events, to encrypt/decrypt messages, ...

## Examples

Here you can find some simple use cases of the library.

## Android App

Here are the first efforts of developing a full Nostr Social client for Android.

It's basically abandoned in favor of [Nostroid](https://github.com/Giszmo/nostroid) and absent
others putting work into it, it will be removed from this repo.

## NostrRelay

This is a full relay implementation.

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