jvcs - Java Version Control System
====================================

Don't call it "Git in Java." It's not.

jvcs is a bounded, educational reimplementation of the core ideas behind
Git, written in Java 21. It does exactly what it says on the tin and
nothing more.

WHAT IT DOES
------------

Content-addressable object storage. Trees. Commits. Branches. A staging
area. A diff that shows you what changed. That's it.

No networking. No merge algorithms. No rebase. No cherry-pick. No hooks.
No submodules. No packfiles. No protocols. If you need any of those, use
the real thing. This is not a replacement.

COMMANDS
--------

  init          Initialize a repository
  add           Stage files
  status        Show working tree state
  commit        Record staged changes
  log           Walk commit history
  branch        List, create, or delete branches
  checkout      Switch branches (updates your working tree)
  diff          Show unstaged changes (working tree vs index)
  hash-object   Hash a file and store it as a blob
  cat-file      Print the contents of a blob
  write-tree    Build a tree object from the working directory
  commit-tree   Low-level commit creation

USAGE
-----

  jvcs init
  jvcs add <file>...
  jvcs commit -m "message"
  jvcs log
  jvcs branch <name>
  jvcs checkout <name>

Standard stuff. If you've used Git, you already know how this works.

ARCHITECTURE
------------

Four layers. No more.

  jvcs.objects    Blob, Tree, Commit. Immutable. Content-addressable.
                  Hash is derived from content. That's the whole point.

  jvcs.store      ObjectStore. Envelope format. Zlib compression. The
                  filesystem boundary. Nothing clever.

  jvcs.repository Repository. Knows where .jvcs lives. Opens and inits.
                  Not a god object, not a service locator, not a manager.

  jvcs.cli        CliParser. Dispatches strings to commands. No magic.

Everything else (index, refs, commands) sits between these layers where
it belongs.

DESIGN PHILOSOPHY
-----------------

  - No abstract interfaces that exist just so you can say you used one.
  - No manager classes, no service locators, no dependency injection.
  - No "utils" dumping grounds.
  - Immutable domain objects where it matters (most places).
  - Mutable state where it makes sense (index, refs).
  - Packages are conceptual boundaries, not namespace inflation.

This is what happens when you let the domain drive the architecture
instead of the other way around.

THINGS THIS PROJECT DOES NOT HAVE
----------------------------------

  - Tests that mock the filesystem
  - A "provider" or "factory" for every bloody thing
  - Five-line methods that should be one line
  - XML configuration
  - A plugin system
  - A contributing.md longer than the readme
  - Your name on a commit that touched three spaces

WHAT YOU CAN LEARN FROM THIS CODE
----------------------------------

  - How content-addressable storage actually works
  - Why immutable objects matter in practice
  - When NOT to use inheritance
  - When NOT to use interfaces
  - That a value object with validation is better than a string
  - That architecture is about boundaries, not layers
  - That "enterprise patterns" are often just indirection in a suit

BUILDING
--------

  mvn package
  java -jar target/jvcs-0.1.jar

Java 21. Maven. Zero dependencies (except JUnit for tests).

--linus
