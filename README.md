# 图书管理系统

Vue 3 + Element Plus前端，Spring Boot 3.2.5 + MyBatis后端，MySQL 8数据库。功能包括用户与权限、馆藏与分类、借还、预约、借阅规则、罚金和统计。

## 目录

| 目录 | 内容 |
|---|---|
| vue_front/ | 前端页面与API调用 |
| springboot_back/ | 后端业务、Mapper和自动化测试 |
| migrations/ | 保留旧数据的规则快照升级脚本 |
| docs/ | 启动、联调、测试报告及历史证据 |
| scripts/ | 本地验收、性能和压力测试工具 |

## 启动

准备JDK17、Node和MySQL8。根据 `springboot_back/src/main/resources/application-local.example.yml` 创建被Git忽略的 `application-local.yml`，填写自己的库名和凭据。

在项目根目录PowerShell启动后端：

```powershell
mvn -f springboot_back/pom.xml spring-boot:run '-Dspring-boot.run.profiles=local'
```

另一个终端启动前端：

```powershell
cd vue_front
npm ci
npm run dev
```

前端默认5173，后端默认8080，以终端实际输出为准。新建数据库和旧库升级方式不同，先阅读[本地启动说明](docs/本地启动与存档说明.md)和[联调说明](docs/组员联调说明.md)。`database.sql`含删表操作，不要在已有业务库中重跑。

## 设计与验证

[实施说明](IMPLEMENTATION.md)介绍规则快照、罚金和统计设计；[文档入口](docs/README.md)汇总最新报告与未完成项；[测试工具说明](scripts/README.md)解释隔离测试库、计时方式和脚本限制。

本轮33项后端自动化、单人页面性能及10人统计查询压力验收通过。借还/预约仍有4类已复现的写流程并发问题，详见回归报告，不能将统计压力通过解释为全系统并发安全。真人易用性暂未执行。

数据库内容、个人连接配置、依赖、构建产物、临时日志和本机备份不会随Git分发。
