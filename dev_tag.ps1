# Get version from gradle.properties
$gradleFile = "gradle.properties"
if (-Not (Test-Path $gradleFile)) {
    Write-Error "gradle.properties not found"
    exit 1
}

$versionLine = Get-Content $gradleFile | Where-Object { $_ -match '^mod_version=' }
if (-Not $versionLine) {
    Write-Error "mod_version not found in gradle.properties"
    exit 1
}

$version = ($versionLine -split '=')[1].Trim()

# Get git short hash
try {
    $gitHash = git rev-parse --short HEAD
} catch {
    Write-Error "Could not get git hash. Are you in a git repository?"
    exit 1
}

# Clean version
$safeVersion = -join ($version.ToCharArray() | Where-Object { $_ -match '[a-zA-Z0-9._-]' })

# Construct tag
$tag = "v${safeVersion}-dev-${gitHash}"

Write-Host "Creating and pushing tag: $tag"
git tag -a $tag -m "Dev build $tag"
git push origin "refs/tags/$tag"
Write-Host "Tag successfully pushed: $tag"
