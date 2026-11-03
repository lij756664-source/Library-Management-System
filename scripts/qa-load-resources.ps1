$qaBackendPid = ((Get-Content -LiteralPath 'tmp/load-processes.json' -Raw | ConvertFrom-Json) | Where-Object name -eq 'load-backend').pid
$qaPrevious = @{}
$qaCpuCount = [Environment]::ProcessorCount
while ($true) {
    $qaNow = [DateTimeOffset]::UtcNow
    $qaRows = @()
    $qaTargets = @(Get-Process -Id $qaBackendPid -ErrorAction SilentlyContinue) + @(Get-Process -Name mysqld -ErrorAction SilentlyContinue)
    foreach ($qaTarget in $qaTargets) {
        $qaCpu = $null
        if ($qaPrevious.ContainsKey($qaTarget.Id)) {
            $qaOld = $qaPrevious[$qaTarget.Id]
            $qaSeconds = ($qaNow - $qaOld.at).TotalSeconds
            $qaCpu = [Math]::Round(100 * ($qaTarget.CPU - $qaOld.cpu) / $qaSeconds / $qaCpuCount, 2)
        }
        $qaRows += @{pid=$qaTarget.Id; name=$qaTarget.ProcessName; cpuPercentOfMachine=$qaCpu; workingSetMiB=[Math]::Round($qaTarget.WorkingSet64/1MB,1); privateMiB=[Math]::Round($qaTarget.PrivateMemorySize64/1MB,1)}
        $qaPrevious[$qaTarget.Id] = @{at=$qaNow; cpu=$qaTarget.CPU}
    }
    $qaState = if (Test-Path -LiteralPath 'docs/2026-11-03多人压力测试/运行状态.json') { Get-Content -LiteralPath 'docs/2026-11-03多人压力测试/运行状态.json' -Raw | ConvertFrom-Json } else { $null }
    @{at=$qaNow.ToString('o');phase=$qaState.phase;logicalProcessors=$qaCpuCount;processes=$qaRows} | ConvertTo-Json -Depth 5 -Compress | Add-Content -LiteralPath 'docs/2026-11-03多人压力测试/资源采样.jsonl' -Encoding UTF8
    if ($qaState.phase -in @('complete','error')) { break }
    Start-Sleep -Seconds 5
}
