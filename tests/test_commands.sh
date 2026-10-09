#!/usr/bin/env bash

echo " 1. Пересборка тестовых архивов "
javac -d out tests/FixtureGenerator.java
java -cp out FixtureGenerator

echo " 2. Прогон стартового скрипта с 3-уровневым VFS "
./run.sh --vfs tests/vfs_nested.zip --script tests/startup_stage4.txt