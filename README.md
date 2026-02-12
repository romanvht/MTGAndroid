# MTGAndroid

Приложение для Android, которое локально запускает [mtg](https://github.com/9seconds/mtg).

MTGAndroid не является VPN. Оно не передает ничего на удаленный сервер, а создает локальный MTProto прокси.

---

### Использование
* В настройках укажите домен, на основе которого будет сгенерирован секрет
* Перейдите на главный экран и нажмите подключить
* Используйте сгенерированную ссылку для подключения в Telegram

### Сборка
1. Клонируйте репозиторий:
```bash
git clone https://github.com/romanvht/MTGAndroid
cd MTGAndroid
```

2. **Важно!** Перед сборкой запустите скрипт загрузки бинарных файлов mtg:

Для Windows:
```cmd
download_binaries.bat
```

Для Linux/macOS:
```bash
./download_binaries.sh
```

Этот скрипт загрузит предкомпилированные бинарные файлы mtg версии 2.1.8 для всех поддерживаемых архитектур Android и разместит их в `app/src/main/jniLibs/`.

3. Соберите проект:
```bash
./gradlew assembleRelease
```

4. APK будет в `app/build/outputs/apk/release/`

### Благодарность
- [mtg](https://github.com/9seconds/mtg)