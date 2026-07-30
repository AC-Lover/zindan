# Публикация релиза на GitHub

Подписанный APK собирается и публикуется **с локального диска**. Приватный ключ не
передаётся GitHub. Подробности о сертификате и резервной копии ключа приведены в
[SIGNING.md](SIGNING.md).

## Перед публикацией

1. Обновите `version.properties`, указав новые `VERSION_NAME` и `VERSION_CODE`.
2. Добавьте соответствующую секцию в `CHANGELOG.md`.
3. Соберите и проверьте APK:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass `
     -File .\tools\build_local_release.ps1
   ```

4. Проверьте приложение на устройстве. Не удаляйте установленный Zindan и рабочий
   профиль. При сомнениях сначала выполните `tools\check_apk_update_compatibility.ps1`.
5. Закоммитьте изменения и отправьте ветку `main`. Рабочее дерево должно быть чистым.
6. Опубликуйте релиз одной командой:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass `
     -File .\tools\publish_local_release.ps1
   ```

Скрипт повторно собирает release APK, проверяет сертификат, создаёт SHA-256, тег вида
`v{VERSION_NAME}-{VERSION_CODE}` и GitHub Release. На GitHub отправляются только тег,
APK, checksum и текст релиза.

## Текст релиза

Если в корне есть `RELEASE_NOTES_v{VERSION_NAME}-{VERSION_CODE}.md`, используется он.
Иначе текст автоматически извлекается из соответствующей секции `CHANGELOG.md`.

GitHub workflow `Release tag check` только проверяет соответствие тега файлу
`version.properties`. Он не собирает и не подписывает APK.
