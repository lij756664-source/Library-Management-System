# 本地验收脚本

这些脚本用于Windows本机课程验收，不是项目启动步骤。默认依赖MySQL 8命令行安装路径、本地配置及已有测试夹具；执行前检查脚本中的库名、端口和证据输出路径。

数据库凭据由 `qa-config.cjs` 从环境变量或被忽略的 `application-local.yml` 读取，不提交个人配置。演示账号不是通用部署凭据。不要把完整令牌、数据库导出或个人配置写进报告。

| 脚本 | 用途与注意事项 |
|---|---|
| qa-regression-20261103.cjs | 本轮回归主脚本；`--rollback-only`、`--supplement`、`--concurrency-more`分别补测回滚、统计边界、并发组合。只对book_db_regression写入；包含临时触发器及专用夹具，必须使用隔离库。会恢复规则，保留专用数据；重跑前另设证据目录避免覆盖历史结果 |
| qa-regression-report.cjs | 读取原始回归结果、合并同ID补测并生成报告；同时只读查询数据库提供夹具清单。为首轮历史报告生成器，后续D05及性能进度以docs/README为准 |
| qa-perf-current-prepare.cjs | 基于现有数据生成器创建新的book_db_perf_20261103，已有库则拒绝覆盖。写独立预期及tmp下代理；不加旧性能脚本的额外调优索引 |
| qa-performance-start.cjs | 在根目录运行；默认启动单人性能后端8082与代理5177，传`--load`则启用多人证据目录。先确认端口空闲、已构建jar/dist、性能库已准备；证据存在时拒绝覆盖 |
| qa-perf-current-verify.cjs | 校验25次页面测量及独立预期，生成本轮单人性能报告；不主动操作浏览器 |
| qa-load-20261103.cjs | 创建独立性能库的20个ADMIN测试账号，执行5人20秒、10人300秒、20人120秒查询及恢复检查；只发统计读请求。新建请求记录存在则拒绝覆盖 |
| qa-load-resources.ps1 | 从本轮启动记录读取PID，每5秒采样Java/MySQL资源；测试结束自动退出。受限环境可能读不到进程，应如实标注缺失并补充只读权限采样 |
| qa-load-report-20261103.cjs | 全部阶段完成后校验页面与接口结果、汇总资源并生成压力报告。10人是正式门槛，20人只作摸底 |
| qa-pie-perf-probe.js | 代理加载的只读页面观察探针，真实点击开始计时，等请求、Vue绘制机会及遮罩结束后读取实际SVG/图例/表格 |

启动本轮性能环境前，在根目录构建：

```powershell
$env:JAVA_HOME = Join-Path $env:USERPROFILE '.jdks\temurin-17.0.20.1'
mvn -f springboot_back/pom.xml '-DskipTests' package
npm --prefix vue_front run build
node scripts/qa-performance-start.cjs --load
```

首次性能数据准备先阅读 `qa-perf-current-prepare.cjs`，确认新库名不会与已有库冲突。历史夹具固定日期，跨日复测需检查逾期基线；不能不核对就套用旧预期。多人脚本会校验jar与上次性能构建哈希，代码变更后应先更新相应验证材料。

压力过程另开终端运行 `node scripts/qa-load-20261103.cjs`；真实浏览器访问5177并操作统计页，页面抽查不是接口脚本自动完成。结束后按tmp下本轮记录的PID并核对命令行关闭专用服务，不能按java/node进程名批量结束用户其他服务。tmp和完整数据库备份均在Git忽略范围。

旧 `qa-concurrency.cjs`、`qa-pickup-concurrency.cjs` 及生成它的 `qa-pickup-build.cjs` 依赖规则行锁制造重叠，不适用于本轮移除规则锁后的并发验收。旧性能验证脚本固定读取历史docs路径；新测试应另设证据目录，勿直接覆盖旧报告。其余旧脚本按原报告版本理解。
