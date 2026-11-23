param([Parameter(Mandatory=$true)][string]$ServerLibraries)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$version = ([xml](Get-Content -Raw pom.xml)).project.version
$env:LOBBY_VERSION = $version
$artifact = "target/LobbySystem-$version.jar"
$jars = @(Get-ChildItem -LiteralPath $ServerLibraries -Recurse -File -Filter '*.jar')
if (-not ($jars.Name -match '^(spigot|paper)-api-')) { throw 'Keine Spigot/Paper-API in ServerLibraries gefunden.' }
$classpath = ($jars.FullName -join [IO.Path]::PathSeparator)
New-Item -ItemType Directory -Force 'target/classes','target/test-classes' | Out-Null
$sources = @(Get-ChildItem 'src/main/java' -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac --release 21 -encoding UTF-8 -cp $classpath -d target/classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Java-Kompilierung fehlgeschlagen.' }
Copy-Item 'src/main/resources/*' 'target/classes' -Recurse -Force
$drivers = @($jars | Where-Object Name -Match '^(gson-|sqlite-jdbc-|mysql-connector-j-|protobuf-java-|slf4j-api-)')
$env:LOBBY_BUNDLE_JARS = $drivers.FullName -join [IO.Path]::PathSeparator
& java tools/Package.java
if ($LASTEXITCODE -ne 0) { throw 'Paketierung fehlgeschlagen.' }
$tests = @(Get-ChildItem 'src/test/java' -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac --release 21 -encoding UTF-8 -cp "target/classes;$classpath" -d target/test-classes $tests
if ($LASTEXITCODE -ne 0) { throw 'Test-Kompilierung fehlgeschlagen.' }
& java --enable-native-access=ALL-UNNAMED -cp "target/test-classes;$artifact;$classpath" de.sujuc.lobby.CoreTest
if ($LASTEXITCODE -ne 0) { throw 'Tests fehlgeschlagen.' }
Write-Output "Erstellt: $artifact"
