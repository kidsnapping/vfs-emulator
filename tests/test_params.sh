#!/usr/bin/env bash

echo " 1. Запуск без параметров "
./run.sh <<< "exit"

echo " 2. Запуск только с VFS "
./run.sh --vfs my_system.zip <<< "exit"

echo " 3. Запуск только со скриптом "
./run.sh --script tests/startup.txt

echo " 4. Запуск с VFS и скриптом "
./run.sh --vfs archive.zip --script tests/startup.txt