#!/bin/bash
# 编译并启动 springboot_back（本机无 Maven，JDK 24 + lombok 手动编译）
# 用法: bash run-backend.sh
# 后端启动在 http://localhost:8080
set -e
cd "$(dirname "$0")/springboot_back"

JDK=/d/Java
M2=/c/Users/Karina/.m2/repository
LOMBOK="$M2/org/projectlombok/lombok/1.18.38/lombok-1.18.38.jar"

# 1. 源码清单
find src/main/java -name '*.java' > /tmp/sources.txt

# 2. jar 依赖清单 -> 分号连接的 Windows 绝对路径，单行（排除 sources/javadoc）
find "$M2" -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' \
  | while read -r j; do cygpath -w "$j"; done | tr '\n' ';' > /tmp/cp.txt

# 3. 编译（--release 17 与 -parameters 都必需；lombok 必须走 -processorpath）
"$JDK/bin/javac" --release 17 -parameters -encoding UTF-8 \
  -processorpath "$LOMBOK" \
  -d target/classes -cp "$(cat /tmp/cp.txt)" \
  "@$(cygpath -w /tmp/sources.txt)"

# 4. 拷贝资源（mapper XML、application.yml）
cp -r src/main/resources/* target/classes/

# 5. 运行 classpath = target/classes + jars
{ cygpath -w "$PWD/target/classes"; printf ';'; cat /tmp/cp.txt; } > /tmp/cp-run.txt
printf -- '-cp' > /tmp/arg1.txt

echo "== 编译完成，启动后端 (http://localhost:8080) =="
exec "$JDK/bin/java" "@$(cygpath -w /tmp/arg1.txt)" "@$(cygpath -w /tmp/cp-run.txt)" com.example.book.BookApplication
