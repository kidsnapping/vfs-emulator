#!/usr/bin/env bash

echo " Генерация тестовых архивов "
javac -d out tests/FixtureGenerator.java
java -cp out FixtureGenerator

echo "1. Тест: Минимальный архив "
./run.sh --vfs tests/vfs_minimal.zip <<< "vfs-info
exit"

echo "2. Тест: Архив с motd и несколькими файлами "
./run.sh --vfs tests/vfs_multi.zip <<< "vfs-info
exit"

echo "3. Тест: Вложенность не менее 3 уровней "
./run.sh --vfs tests/vfs_nested.zip <<< "vfs-info
exit"

echo "4. Тест: Обработка ошибки несуществующего файла "
./run.sh --vfs tests/does_not_exist.zip <<< "exit"

echo "5. Тест: Обработка ошибки неверного формата "
./run.sh --vfs tests/vfs_corrupted.zip <<< "exit"

echo "6. Тест: Стартовый скрипт этапа 3 "
./run.sh --vfs tests/vfs_multi.zip --script tests/startup_stage3.txt