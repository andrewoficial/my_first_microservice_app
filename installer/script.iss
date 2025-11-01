#define MyAppName "Elephant Monitor"
#define MyAppVersion "1.8.31"
#define MyAppPublisher "Andrew Official"
#define MyAppURL "https://github.com/andrewoficial/my_first_microservice_app/"

[Setup]
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName=C:\ElephantMonitor
OutputDir=output
OutputBaseFilename=ElephantMonitor_Setup
Compression=lzma
SolidCompression=yes
SetupIconFile=sources\favicon.ico
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
SetupLogging=yes

[Files]
Source: "../target/Elephant-Monitor-{#MyAppVersion}.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "../sources/favicon.ico"; DestDir: "{app}"; Flags: ignoreversion
Source: "../sources/launcher.exe"; DestDir: "{app}"; Flags: ignoreversion

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\launcher.exe"; WorkingDir: "{app}"; IconFilename: "{app}\favicon.ico"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\launcher.exe"; WorkingDir: "{app}"; IconFilename: "{app}\favicon.ico"; Tasks: desktopicon

[Run]
Filename: "https://github.com/andrewoficial/my_first_microservice_app/blob/master/README.md"; Description: "View documentation"; Flags: postinstall shellexec unchecked

[UninstallDelete]
Type: filesandordirs; Name: "{app}"
