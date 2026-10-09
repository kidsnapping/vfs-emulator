#!/usr/bin/env bash
mkdir -p out
javac -d out src/ru/mirea/emulator/Main.java
java -cp out ru.mirea.emulator.Main "$@"