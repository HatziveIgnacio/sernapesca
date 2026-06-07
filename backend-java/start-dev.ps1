$env:JAVA_HOME = "C:\Java\jdk21.0.10_7"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH

$mavenVersion = "3.9.9"
$wrapperDir   = Join-Path $PSScriptRoot ".mvn\wrapper"
$mavenDir     = Join-Path $wrapperDir "apache-maven-$mavenVersion"
$mavenZip     = Join-Path $wrapperDir "apache-maven-$mavenVersion-bin.zip"
$mavenUrl     = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip"

if (-not (Test-Path "$mavenDir\bin\mvn.cmd")) {
    Write-Host "[mvnw] Descargando Apache Maven $mavenVersion..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip -UseBasicParsing
    Expand-Archive -Path $mavenZip -DestinationPath $wrapperDir -Force
    Remove-Item $mavenZip
    Write-Host "[mvnw] Maven listo." -ForegroundColor Green
}

Write-Host "[mvnw] Iniciando backend Java (perfil dev, H2 en memoria)..." -ForegroundColor Cyan
& "$mavenDir\bin\mvn.cmd" spring-boot:run "-Dspring-boot.run.profiles=dev"
