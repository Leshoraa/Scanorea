# Project Rules

## Android device testing

- Dilarang menggunakan `adb` dan emulator / Android virtual device (AVD).
- Jangan menjalankan perintah `adb`, jangan mengontrol emulator, mengambil screenshot lewat adb, atau menginstall APK ke emulator.
- Pengujian perangkat (device testing) dilakukan langsung oleh pengguna (user).
- Gradle tasks (compile, unit tests, lint, assemble) tetap diperbolehkan.

## User modifications

- Dilarang mengubah, menimpa (overwrite), atau mengembalikan (revert) perubahan kode/file yang dilakukan/diedit sendiri oleh pengguna (user).
- Selalu hormati dan pertahankan setiap perubahan manual dari pengguna.
