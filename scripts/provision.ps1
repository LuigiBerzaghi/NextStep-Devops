Param(
  [string]$Location = "brazilsouth",
  [string]$ResourceGroup = "rg-nextstep",
  [string]$SqlServerName = "sqlnextstep",         # precisa ser ÃƒÆ’Ã‚Âºnico na Azure
  [string]$DbName = "dbnextstep",                 #valores usados caso o usuÃƒÆ’Ã‚Â¡rio nÃƒÆ’Ã‚Â£o digite nada
  [string]$AdminUser = "adminuser",
  [string]$AdminPass = "SenhaSuperSegura123!",
  [switch]$AllowAzureServices = $true,
  [switch]$AllowClientIP = $true,
  [string]$Plan = "planNextstep",
  # ParÃƒÆ’Ã‚Â¢metros para Web App em contÃƒÆ’Ã‚Âªiner (ACR)
  [string]$WebAppName = "nextstep-2TDSB",
  [string]$AcrName = "acrnextstep",
  [string]$ImageRepo = "nextstep",
  [string]$ImageTag = "latest"
)

Write-Host "==> Criando Resource Group $ResourceGroup em $Location "
az group create -n $ResourceGroup -l $Location | Out-Null

Write-Host "==> Criando SQL Server $SqlServerName "
az sql server create `
  -g $ResourceGroup `
  -n $SqlServerName `
  -u $AdminUser `
  -p $AdminPass `
  -l $Location | Out-Null

Write-Host "==> Criando Database $DbName "
az sql db create `
  -g $ResourceGroup `
  -s $SqlServerName `
  -n $DbName `
  --service-objective S0 `
  --backup-storage-redundancy Local | Out-Null

if ($AllowAzureServices) {
  Write-Host "==> Liberando Azure Services (0.0.0.0)"
  az sql server firewall-rule create `
    -g $ResourceGroup -s $SqlServerName `
    -n AllowAzureServices `
    --start-ip-address 0.0.0.0 `
    --end-ip-address 0.0.0.0 | Out-Null
}

if ($AllowClientIP) {
  $ip = (Invoke-RestMethod -Uri "https://api.ipify.org?format=text" -TimeoutSec 10)
  Write-Host "==> Liberando IP do cliente: $ip ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦"
  az sql server firewall-rule create `
    -g $ResourceGroup -s $SqlServerName `
    -n AllowMyIP `
    --start-ip-address $ip `
    --end-ip-address $ip | Out-Null
}

# Monta JDBC 
$serverFqdn = "$SqlServerName.database.windows.net"
$jdbc = "jdbc:sqlserver://$serverFqdn"+":1433;database=$DbName;user=$AdminUser@$SqlServerName;password=$AdminPass;encrypt=true;trustServerCertificate=false;loginTimeout=30;"

Write-Host "==> Definindo variÃƒÆ’Ã‚Â¡veis de ambiente"
# Define variÃƒÆ’Ã‚Â¡veis de ambiente
$env:SPRING_DATASOURCE_URL = $jdbc
$env:SPRING_DATASOURCE_USERNAME = $AdminUser
$env:SPRING_DATASOURCE_PASSWORD = $AdminPass
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver"

az provider register --namespace Microsoft.Web

Write-Host "==> criando o plano do serviÃƒÆ’Ã‚Â§o de aplicativo"
az appservice plan create -g $ResourceGroup -n $Plan -l $Location --sku B1 --is-linux
Write-Host "==> Garantindo Azure Container Registry $AcrName"
$acrDesiredName = $AcrName.ToLower()
if ($acrDesiredName -ne $AcrName) {
  Write-Warning "Nome do ACR deve ser minusculo. Usando $acrDesiredName."
}
$acrJson = az acr show -n $acrDesiredName --query "{name:name,resourceGroup:resourceGroup,loginServer:loginServer}" -o json 2>$null
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($acrJson)) {
  Write-Host "   -> ACR nao encontrado. Criando..."
  az acr create -n $acrDesiredName -g $ResourceGroup -l $Location --sku Basic | Out-Null
  if ($LASTEXITCODE -ne 0) {
    throw "Falha ao criar Azure Container Registry $acrDesiredName"
  }
  $acrJson = az acr show -n $acrDesiredName --query "{name:name,resourceGroup:resourceGroup,loginServer:loginServer}" -o json
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
az acr update -n $acrEffectiveName --admin-enabled true | Out-Null

Write-Host "==> Criando o serviÃƒÆ’Ã‚Â§o de aplicativo"
# Resolver login server do ACR e imagem completa
$acrLoginServer = $acrInfo.loginServer
$imageRef = "$acrLoginServer"+"/$ImageRepo"+":$ImageTag"

# Criar Web App com imagem ÃƒÆ’Ã‚Âºnica (-i)
az webapp create -g $ResourceGroup -p $Plan -n $WebAppName -i $imageRef

Write-Host "==> Configurando contÃƒÆ’Ã‚Âªiner com credenciais do ACR"
$acrUser = az acr credential show -n $acrEffectiveName --query username -o tsv
$acrPass = az acr credential show -n $acrEffectiveName --query passwords[0].value -o tsv
az webapp config container set -g $ResourceGroup -n $WebAppName -i $imageRef -r $acrLoginServer -u $acrUser -p $acrPass | Out-Null

Write-Host "==> Definindo configuraÃƒÆ’Ã‚Â§ÃƒÆ’Ã‚Âµes do WebApp"
az webapp config appsettings set -g $ResourceGroup -n $WebAppName --settings `
  SPRING_DATASOURCE_URL=$jdbc `
  SPRING_DATASOURCE_USERNAME=$AdminUser `
  SPRING_DATASOURCE_PASSWORD=$AdminPass `
  SPRING_DATASOURCE_DRIVER_CLASS_NAME="com.microsoft.sqlserver.jdbc.SQLServerDriver" `
  WEBSITES_PORT=8080

# Deploy via container image configured acima (sem deploy JAR)

Write-Host "==> Acesse: https://$WebAppName.azurewebsites.net/motos"