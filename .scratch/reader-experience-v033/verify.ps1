param([switch]$All)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$verificationRoot = Join-Path $env:TEMP 'Yomu-v033-verification'
New-Item -ItemType Directory -Path $verificationRoot -Force | Out-Null
foreach ($relativeFile in @('settings.gradle.kts', 'build.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat', 'app/build.gradle.kts', 'app/proguard-rules.pro')) {
    $targetFile = Join-Path $verificationRoot $relativeFile
    New-Item -ItemType Directory -Path (Split-Path $targetFile) -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot $relativeFile) -Destination $targetFile -Force
}
foreach ($relativeDirectory in @('app/src', 'app/schemas', 'gradle')) {
    $targetDirectory = Join-Path $verificationRoot $relativeDirectory
    New-Item -ItemType Directory -Path $targetDirectory -Force | Out-Null
    Copy-Item -Path (Join-Path $projectRoot "$relativeDirectory/*") -Destination $targetDirectory -Recurse -Force
}
$env:JAVA_HOME = Join-Path $projectRoot '.jdk/jdk-17.0.20+8'
$env:ANDROID_HOME = Join-Path $projectRoot '.android-sdk'
$gradleArguments = @('-p', $verificationRoot, '--console=plain', '--max-workers=2')
if ($All) {
    $gradleArguments += @('testDebugUnitTest', 'lintDebug', 'assembleDebug')
} else {
    $gradleArguments += @('testDebugUnitTest', '--tests', 'cn.yomu.reader.ui.ZoomGestureTest', '--tests', 'cn.yomu.reader.ui.TextFieldEdgeScrollTest')
}
& (Join-Path $verificationRoot 'gradlew.bat') @gradleArguments
exit $LASTEXITCODE
