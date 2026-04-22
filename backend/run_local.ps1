# Load .env from root if it exists
if (Test-Path "../.env") {
    Get-Content "../.env" | ForEach-Object {
        if ($_ -match "^\s*([^#=]+)\s*=\s*(.*)\s*$") {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            # Remove quotes if present
            $value = $value -replace "^['""]|['""]$", ""
            [System.Environment]::SetEnvironmentVariable($name, $value)
        }
    }
}

# Backend Environment Configuration
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://db.yobbbbpyfxutcejelmat.supabase.co:5432/postgres"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = $env:SEND_DB_PASSWORD
$env:APP_AUTH_SUPABASE_ISSUER_URI = $env:APP_AUTH_SUPABASE_ISSUER_URI
$env:APP_AUTH_SUPABASE_JWK_SET_URI = $env:APP_AUTH_SUPABASE_JWK_SET_URI
$env:APP_AUTH_SUPABASE_JWT_SECRET = $env:APP_AUTH_SUPABASE_JWT_SECRET
$env:APP_AUTH_SUPABASE_AUDIENCE = $env:APP_AUTH_SUPABASE_AUDIENCE
$env:APP_LECTURES_PROGRESS_COOKIE_SECRET = $env:APP_LECTURES_PROGRESS_COOKIE_SECRET
$env:SERVER_PORT = "8080"
$env:DATABASE_BOOTSTRAP_DATA_DIRECTORY = (Resolve-Path "../data").Path

# Build and Run
./gradlew bootRun
