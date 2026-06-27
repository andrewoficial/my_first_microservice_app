@echo on
echo Run release script bat file

:: Вывод переданного аргумента
echo First argument: "%~1"

:: Проверка аргумента
if "%~1"=="" (
    echo Usage: release.bat "version"
    exit /b 1
)

:: Установка версии
echo Setup version
set "VERSION=%~1"

:: Установка пути к JAR-файлу
echo Setup file link
set "JAR_FILE=target\Elephant-Monitor-%VERSION%.jar"
set "INFO_FILE=target\metaData.info"

:: Диагностика переменных
echo VERSION = %VERSION%
echo JAR_FILE = %JAR_FILE%

:: Проверка наличия JAR-файла
echo Run check file existing
if not exist "%JAR_FILE%" (
    echo JAR file not found: %JAR_FILE%
    exit /b 1
)

:: Если файл найден
echo File found
echo Send command to GH

:: Команда для создания релиза
gh release create "v%VERSION%" "%JAR_FILE%" "%INFO_FILE%" --title "Release v%VERSION%" --notes "Automatic release"
if %ERRORLEVEL% neq 0 (
    echo GitHub release creation failed.
    exit /b 1
)

echo Release created successfully
