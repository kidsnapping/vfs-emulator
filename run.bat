@echo off
if not exist out mkdir out
javac -d out src\ru\mirea\emulator\Main.java
java -cp out ru.mirea.emulator.Main %*