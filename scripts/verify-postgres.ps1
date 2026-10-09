param(
    [string]$PostgresBin = 'C:\Program Files\PostgreSQL\17\bin',
    [string]$JavaHome = 'C:\Users\antel\.jdks\temurin-21.0.12',
    [string]$MavenRepository = '',
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
foreach ($exe in @('initdb.exe', 'pg_ctl.exe', 'createdb.exe')) {
    if (!(Test-Path (Join-Path $PostgresBin $exe))) { throw "No se encontró $exe en $PostgresBin" }
}
if (!(Test-Path (Join-Path $JavaHome 'bin\java.exe'))) { throw 'Indique un JDK 21 válido en -JavaHome' }
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ('gastro-pg-test-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot | Out-Null
$dataDir = Join-Path $testRoot 'data'
$testLog = Join-Path $testRoot 'postgres.log'
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
$listener.Start()
$testPort = $listener.LocalEndpoint.Port
$listener.Stop()
$savedEnv = @{}
foreach ($name in @('JAVA_HOME', 'Path', 'TEST_DB_URL', 'TEST_DB_USERNAME', 'TEST_DB_PASSWORD')) {
    $savedEnv[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$started = $false
$verifyExit = 1
try {
    # Instancia temporal, sólo loopback, sin datos ni claves reales. No usa el servicio instalado.
    & (Join-Path $PostgresBin 'initdb.exe') -D $dataDir -U gastro_test --auth=trust --encoding=UTF8 --no-locale
    if ($LASTEXITCODE -ne 0) { throw 'initdb no pudo crear la instancia temporal' }
    & (Join-Path $PostgresBin 'pg_ctl.exe') -D $dataDir -l $testLog -o "-h 127.0.0.1 -p $testPort" -w -t 30 start
    if ($LASTEXITCODE -ne 0) { throw "PostgreSQL temporal no inició. Consulte $testLog" }
    $started = $true
    & (Join-Path $PostgresBin 'createdb.exe') -h 127.0.0.1 -p $testPort -U gastro_test gastro_backend_test
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base de prueba' }
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;" + $savedEnv['Path']
    $env:TEST_DB_URL = "jdbc:postgresql://127.0.0.1:$testPort/gastro_backend_test"
    $env:TEST_DB_USERNAME = 'gastro_test'
    $env:TEST_DB_PASSWORD = ''
    $mavenArgs = @('verify', '-Dspring.profiles.active=postgres-test')
    if ($MavenRepository) { $mavenArgs += "-Dmaven.repo.local=$MavenRepository" }
    if ($Offline) { $mavenArgs += '-o' }
    Push-Location $projectRoot
    try {
        & '.\mvnw.cmd' @mavenArgs
        $verifyExit = $LASTEXITCODE
        $reports = Join-Path $projectRoot 'target\surefire-reports'
        if (Test-Path $reports) { Copy-Item -LiteralPath $reports -Destination (Join-Path $testRoot 'reports') -Recurse }
    } finally { Pop-Location }
} finally {
    if ($started) {
        & (Join-Path $PostgresBin 'pg_ctl.exe') -D $dataDir -m fast -w -t 30 stop
        if ($LASTEXITCODE -ne 0) { $verifyExit = 1; Write-Warning "Revise el apagado de la instancia en $dataDir" }
    }
    foreach ($name in $savedEnv.Keys) { [Environment]::SetEnvironmentVariable($name, $savedEnv[$name], 'Process') }
    Write-Host "Evidencia temporal conservada en $testRoot. No se modificó su base gastroreserva."
}
exit $verifyExit
