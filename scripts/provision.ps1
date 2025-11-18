Param(
  [string]$Location = "brazilsouth",
  [string]$ResourceGroup = "rg-nextstep",
  [string]$SqlServerName = "sqlnextstep",         # precisa ser idêntico na Azure
  [string]$DbName = "dbnextstep",                 #valores usados caso o usuário não digite nada
  [string]$AdminUser = "adminuser",
  [string]$AdminPass = "SenhaSuperSegura123!",
  [switch]$AllowAzureServices = $true,
  [switch]$AllowClientIP = $true,
  [string]$Plan = "planNextstep",
  # Parâmetros para Web App em contâiner (ACR)
  [string]$WebAppName = "nextstep-2TDSB",
  [string]$AcrName = "acrnextstep",
  [string]$ImageRepo = "nextstep",
  [string]$ImageTag = "latest"
)

function Invoke-AzCli {
  param(
    [Parameter(Mandatory = $true)]
    [string[]]$Args,
    [switch]$CaptureOutput
  )

  if (-not $script:AzCliInvoker) {
    $azCmd = Get-Command az -ErrorAction Stop
    $candidate = $azCmd.Source
    if ($candidate -like "*.cmd") {
      $maybePs = Join-Path (Split-Path $candidate -Parent) "azps.ps1"
      if (Test-Path $maybePs) {
        $candidate = $maybePs
      }
    }
    $script:AzCliInvoker = $candidate
  }

  if ($CaptureOutput) {
    $result = & $script:AzCliInvoker @Args
  } else {
    & $script:AzCliInvoker @Args | Out-Null
  }

  $exitCode = $LASTEXITCODE
  if ($exitCode -ne 0) {
    $joined = ($Args -join ' ')
    throw "Falha ao executar: az $joined"
  }

  if ($CaptureOutput) {
    return ($result -join "`n").Trim()
  }
}

Write-Host "==> Criando Resource Group $ResourceGroup em $Location "
Invoke-AzCli @("group","create","-n",$ResourceGroup,"-l",$Location)

Write-Host "==> Criando SQL Server $SqlServerName "
Invoke-AzCli @(
  "sql","server","create",
  "-g",$ResourceGroup,
  "-n",$SqlServerName,
  "-u",$AdminUser,
  "-p",$AdminPass,
  "-l",$Location
)

Write-Host "==> Criando Database $DbName "
Invoke-AzCli @(
  "sql","db","create",
  "-g",$ResourceGroup,
  "-s",$SqlServerName,
  "-n",$DbName,
  "--service-objective","S0",
  "--backup-storage-redundancy","Local"
)

if ($AllowAzureServices) {
  Write-Host "==> Liberando Azure Services (0.0.0.0)"
  Invoke-AzCli @(
    "sql","server","firewall-rule","create",
    "-g",$ResourceGroup,
    "-s",$SqlServerName,
    "-n","AllowAzureServices",
    "--start-ip-address","0.0.0.0",
    "--end-ip-address","0.0.0.0"
  )
}

if ($AllowClientIP) {
  $ip = "179.215.180.66"
  Write-Host "==> Liberando IP do cliente: $ip "
  Invoke-AzCli @(
    "sql","server","firewall-rule","create",
    "-g",$ResourceGroup,
    "-s",$SqlServerName,
    "-n","AllowMyIP",
    "--start-ip-address",$ip,
    "--end-ip-address",$ip
  )
}

# Monta JDBC 
$serverFqdn = "$SqlServerName.database.windows.net"
$jdbc = "jdbc:sqlserver://$serverFqdn"+":1433;database=$DbName;user=$AdminUser@$SqlServerName;password=$AdminPass;encrypt=true;trustServerCertificate=false;loginTimeout=30;"

Write-Host "==> Definindo variáveis de ambiente"
# Define variáveis de ambiente
$env:SPRING_DATASOURCE_URL = $jdbc
$env:SPRING_DATASOURCE_USERNAME = $AdminUser
$env:SPRING_DATASOURCE_PASSWORD = $AdminPass
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver"

Invoke-AzCli @("provider","register","--namespace","Microsoft.Web")

Write-Host "==> Criando o plano do serviço de aplicativo"
Invoke-AzCli @(
  "appservice","plan","create",
  "-g",$ResourceGroup,
  "-n",$Plan,
  "-l",$Location,
  "--sku","B1",
  "--is-linux"
)
Write-Host "==> Garantindo Azure Container Registry $AcrName"
$acrDesiredName = $AcrName.ToLower()
if ($acrDesiredName -ne $AcrName) {
  Write-Warning "Nome do ACR deve ser minusculo. Usando $acrDesiredName."
}
$acrJson = $null
try {
  $acrJson = Invoke-AzCli @(
    "acr","show",
    "-n",$acrDesiredName,
    "--query","{name:name,resourceGroup:resourceGroup,loginServer:loginServer}",
    "-o","json"
  ) -CaptureOutput
} catch {
  $acrJson = $null
}
if ([string]::IsNullOrWhiteSpace($acrJson)) {
  Write-Host "   -> ACR nao encontrado. Criando..."
  Invoke-AzCli @(
    "acr","create",
    "-n",$acrDesiredName,
    "-g",$ResourceGroup,
    "-l",$Location,
    "--sku","Basic"
  )
  $acrJson = Invoke-AzCli @(
    "acr","show",
    "-n",$acrDesiredName,
    "--query","{name:name,resourceGroup:resourceGroup,loginServer:loginServer}",
    "-o","json"
  ) -CaptureOutput
}
if ([string]::IsNullOrWhiteSpace($acrJson)) {
  throw "Nao foi possivel obter dados do ACR $acrDesiredName"
}
$acrInfo = $acrJson | ConvertFrom-Json
$acrEffectiveName = $acrInfo.name
if (-not $acrEffectiveName) {
  throw "Retorno invalido ao consultar ACR $acrDesiredName"
}
if ($acrInfo.resourceGroup -and $acrInfo.resourceGroup -ne $ResourceGroup) {
  Write-Warning "ACR $acrEffectiveName esta no resource group $($acrInfo.resourceGroup). Sera reutilizado mesmo assim."
}
Invoke-AzCli @(
  "acr","update",
  "-n",$acrEffectiveName,
  "--admin-enabled","true"
)

Write-Host "==> Criando o serviço de aplicativo"
# Resolver login server do ACR e imagem completa
$acrLoginServer = $acrInfo.loginServer
$imageRef = "$acrLoginServer/$ImageRepo"+":$ImageTag"

# Obter credenciais do ACR (necessarias para criar/configurar o Web App)
$acrUser = Invoke-AzCli @(
  "acr","credential","show",
  "-n",$acrEffectiveName,
  "--query","username",
  "-o","tsv"
) -CaptureOutput
$acrPass = Invoke-AzCli @(
  "acr","credential","show",
  "-n",$acrEffectiveName,
  "--query","passwords[0].value",
  "-o","tsv"
) -CaptureOutput

# Criar Web App com runtime placeholder suportado (container configurado depois)
Invoke-AzCli @(
  "webapp","create",
  "-g",$ResourceGroup,
  "-p",$Plan,
  "-n",$WebAppName,
  "--runtime","DOTNETCORE:8.0"
)
Write-Host "==> Configurando contâiner com credenciais do ACR"
Invoke-AzCli @(
  "webapp","config","container","set",
  "-g",$ResourceGroup,
  "-n",$WebAppName,
  "-i",$imageRef,
  "-r",$acrLoginServer,
  "-u",$acrUser,
  "-p",$acrPass
)

Write-Host "==> Definindo configurações do WebApp"
Invoke-AzCli @(
  "webapp","config","appsettings","set",
  "-g",$ResourceGroup,
  "-n",$WebAppName,
  "--settings",
  "SPRING_DATASOURCE_URL=$jdbc",
  "SPRING_DATASOURCE_USERNAME=$AdminUser",
  "SPRING_DATASOURCE_PASSWORD=$AdminPass",
  "SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.microsoft.sqlserver.jdbc.SQLServerDriver",
  "WEBSITES_PORT=8080"
)
# Deploy via container image configured acima (sem deploy JAR)

Write-Host "==> Acesse: https://$WebAppName.azurewebsites.net"
