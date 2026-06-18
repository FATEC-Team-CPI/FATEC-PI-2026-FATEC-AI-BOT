# FATEC AI Bot — Docker Compose Helper (PowerShell)
# Uso: .\docker-compose.ps1 -Command setup|build|up|down|logs|health|clean

param(
    [Parameter(Position=0, HelpMessage="Comando a executar")]
    [ValidateSet("setup", "build", "up", "down", "restart", "logs", "health", "clean", "ps", "help")]
    [string]$Command = "help",

    [Parameter(HelpMessage="Serviço específico (backend, mcp, localstack)")]
    [string]$Service,

    [switch]$Help
)

$BLUE = "`e[0;34m"
$GREEN = "`e[0;32m"
$YELLOW = "`e[0;33m"
$RED = "`e[0;31m"
$NC = "`e[0m"

function Show-Help {
    Write-Host "$BLUE╔════════════════════════════════════════════════════════════════╗$NC"
    Write-Host "$BLUE║  FATEC AI Bot — Docker Compose Helper (PowerShell)           ║$NC"
    Write-Host "$BLUE╚════════════════════════════════════════════════════════════════╝$NC"
    Write-Host ""
    Write-Host "$GREEN📋 Uso:$NC"
    Write-Host "  .\docker-compose.ps1 -Command <comando>"
    Write-Host ""
    Write-Host "$GREEN🛠️  Comandos:$NC"
    Write-Host "  setup          Preparar .env"
    Write-Host "  build          Build das imagens Docker"
    Write-Host "  up             Iniciar LocalStack + MCP + Backend"
    Write-Host "  down           Parar todos os serviços"
    Write-Host "  restart        Reiniciar todos os serviços"
    Write-Host "  logs           Ver logs (use -Service backend|mcp|localstack)"
    Write-Host "  health         Checar saúde dos serviços"
    Write-Host "  clean          Remover containers e volumes"
    Write-Host "  ps             Listar containers"
    Write-Host "  help           Mostrar esta ajuda"
    Write-Host ""
    Write-Host "$GREEN📝 Exemplos:$NC"
    Write-Host "  .\docker-compose.ps1 -Command setup"
    Write-Host "  .\docker-compose.ps1 -Command build"
    Write-Host "  .\docker-compose.ps1 -Command up"
    Write-Host "  .\docker-compose.ps1 -Command logs -Service backend"
    Write-Host "  .\docker-compose.ps1 -Command health"
    Write-Host ""
}

function Invoke-Setup {
    Write-Host "$YELLOW📋 Preparando ambiente....$NC"
    if (-not (Test-Path ".env")) {
        Copy-Item ".env.example" ".env"
        Write-Host "$GREEN✅ .env criado a partir de .env.example$NC"
        Write-Host "$YELLOW⚠️  Edite .env e adicione sua GROQ_API_KEY!$NC"
    }
    else {
        Write-Host "$GREEN✅ .env já existe$NC"
    }
}

function Invoke-Build {
    Write-Host "$YELLOW🔨 Building imagens Docker...$NC"
    docker-compose build
    Write-Host "$GREEN✅ Build concluído!$NC"
}

function Invoke-Up {
    Write-Host "$YELLOW🚀 Iniciando serviços...$NC"
    docker-compose up -d
    Write-Host "$GREEN✅ Serviços iniciados!$NC"
    Write-Host ""
    Write-Host "$BLUE⏳ Aguardando serviços ficarem saudáveis...$NC"
    Start-Sleep -Seconds 5
    Invoke-Health
}

function Invoke-Down {
    Write-Host "$YELLOW⛔ Parando serviços...$NC"
    docker-compose down
    Write-Host "$GREEN✅ Serviços parados!$NC"
}

function Invoke-Restart {
    Write-Host "$YELLOW🔄 Reiniciando serviços...$NC"
    docker-compose restart
    Write-Host "$GREEN✅ Serviços reiniciados!$NC"
}

function Invoke-Logs {
    if ($Service) {
        Write-Host "$BLUE📋 Logs de $Service:$NC"
        docker-compose logs -f $Service
    }
    else {
        docker-compose logs -f
    }
}

function Invoke-Health {
    Write-Host ""
    Write-Host "$BLUE🏥 Checando saúde dos serviços...$NC"
    Write-Host ""

    $localstackOk = $false
    $mcpOk = $false
    $backendOk = $false

    try {
        $response = curl -s http://localhost:4566 -UseBasicParsing
        $localstackOk = $true
    }
    catch { }

    try {
        $response = curl -s http://localhost:8001/health -UseBasicParsing
        $mcpOk = $true
    }
    catch { }

    try {
        $response = curl -s http://localhost:8082/health -UseBasicParsing
        $backendOk = $true
    }
    catch { }

    Write-Host -NoNewline "LocalStack: "
    if ($localstackOk) {
        Write-Host "$GREEN✅ OK$NC"
    }
    else {
        Write-Host "$RED❌ DOWN$NC"
    }

    Write-Host -NoNewline "MCP Server: "
    if ($mcpOk) {
        Write-Host "$GREEN✅ OK$NC"
    }
    else {
        Write-Host "$RED❌ DOWN$NC"
    }

    Write-Host -NoNewline "Quarkus Backend: "
    if ($backendOk) {
        Write-Host "$GREEN✅ OK$NC"
    }
    else {
        Write-Host "$RED❌ DOWN$NC"
    }

    Write-Host ""
    Write-Host "$BLUE📡 Endereços úteis:$NC"
    Write-Host "  http://localhost:8082              (Quarkus)"
    Write-Host "  http://localhost:8082/swagger      (Swagger UI)"
    Write-Host "  http://localhost:8001              (MCP Server)"
    Write-Host "  http://localhost:4566              (LocalStack)"
    Write-Host ""
}

function Invoke-Clean {
    Write-Host "$RED🗑️  Removendo containers e volumes...$NC"
    docker-compose down -v
    Write-Host "$GREEN✅ Limpeza concluída!$NC"
}

function Invoke-PS {
    Write-Host "$BLUE📋 Containers em execução:$NC"
    docker-compose ps
}

# Executar comando
switch ($Command) {
    "help" {
        Show-Help
    }
    "setup" {
        Invoke-Setup
    }
    "build" {
        Invoke-Setup
        Invoke-Build
    }
    "up" {
        Invoke-Setup
        Invoke-Build
        Invoke-Up
    }
    "down" {
        Invoke-Down
    }
    "restart" {
        Invoke-Restart
    }
    "logs" {
        Invoke-Logs
    }
    "health" {
        Invoke-Health
    }
    "clean" {
        Invoke-Clean
    }
    "ps" {
        Invoke-PS
    }
    default {
        Show-Help
    }
}
