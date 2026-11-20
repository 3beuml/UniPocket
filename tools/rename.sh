#!/usr/bin/env bash
#
# UniPocket 改名脚本
#
# 把框架的示例包名 / 应用名换成你自己的，之后界面代码一行都不用动。
#
# 用法：
#   ./tools/rename.sh --package com.yourschool.pocket [--app-name "我的课程表"] [--dry-run]
#
# 做的事：
#   1. 把各模块源码目录 com/unipocket 整体移动到新包名路径（用 git mv，保留历史）
#   2. 改写所有 *.kt / *.kts / *.xml / *.toml / *.properties / *.md / *.yml 里的 com.unipocket
#   3. 改写 gradle 根项目名与 res/values/strings.xml 里的 app_name
#
# 不做的事：
#   - 不改类名（UniPocketTheme、UniPocketException 等保持原样，方便日后对比上游）
#   - 不改 README 里的项目介绍（请自行更新署名，MIT 要求保留 LICENSE）
#
set -euo pipefail

OLD_PACKAGE="com.unipocket"
OLD_APP_NAME="UniPocket"

NEW_PACKAGE=""
NEW_APP_NAME=""
DRY_RUN=0

usage() {
  cat <<'EOF'
用法: tools/rename.sh --package <新包名> [--app-name <应用名>] [--dry-run]

示例:
  ./tools/rename.sh --package com.acme.pocket
  ./tools/rename.sh --package com.acme.pocket --app-name "Acme 课程表"
  ./tools/rename.sh --package com.acme.pocket --dry-run

新包名要求: 至少两段、全小写，如 com.acme.pocket
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --package)
      NEW_PACKAGE="${2:-}"
      shift 2
      ;;
    --app-name)
      NEW_APP_NAME="${2:-}"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "未知参数: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$NEW_PACKAGE" ]]; then
  usage
  exit 1
fi

if [[ ! "$NEW_PACKAGE" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]]; then
  echo "错误：包名不合法：$NEW_PACKAGE（要求至少两段、全小写，如 com.acme.pocket）" >&2
  exit 1
fi

if [[ "$NEW_PACKAGE" == "$OLD_PACKAGE" ]]; then
  echo "错误：新包名与旧包名相同，无需改名。" >&2
  exit 1
fi

cd "$(git rev-parse --show-toplevel)"

if ! grep -rq "package $OLD_PACKAGE" --include='*.kt' .; then
  echo "错误：找不到 package $OLD_PACKAGE 的源码，可能已经改过名。" >&2
  exit 1
fi

OLD_PATH="${OLD_PACKAGE//./\/}"
NEW_PATH="${NEW_PACKAGE//./\/}"

echo "旧包名: $OLD_PACKAGE"
echo "新包名: $NEW_PACKAGE"
[[ -n "$NEW_APP_NAME" ]] && echo "新应用名: $NEW_APP_NAME"
[[ $DRY_RUN -eq 1 ]] && echo "模式: 仅预览，不改文件"
echo

# ── 1. 移动源码目录 ──────────────────────────────────────────────────
moved=0
for module in core-domain core-ui widget demo; do
  for src in src/main src/test src/androidTest; do
    old="$module/$src/kotlin/$OLD_PATH"
    [[ -d "$old" ]] || continue
    new="$module/$src/kotlin/$NEW_PATH"
    if [[ $DRY_RUN -eq 1 ]]; then
      echo "[预览] 移动目录 $old -> $new"
    else
      mkdir -p "$(dirname "$new")"
      git mv "$old" "$new"
      echo "移动目录 $old -> $new"
    fi
    moved=$((moved + 1))
  done
done

if [[ $moved -eq 0 ]]; then
  echo "错误：没有找到任何源码目录可移动，脚本可能需要更新。" >&2
  exit 1
fi

# ── 2. 改写文本内容 ──────────────────────────────────────────────────
escape_sed() {
  printf '%s' "$1" | sed -e 's/[\/&|]/\\&/g'
}

NEW_PACKAGE_ESC=$(escape_sed "$NEW_PACKAGE")

changed=0
while IFS= read -r file; do
  [[ -f "$file" ]] || continue
  if grep -q "$OLD_PACKAGE" "$file" 2>/dev/null; then
    if [[ $DRY_RUN -eq 1 ]]; then
      echo "[预览] 改包名 $file"
    else
      sed -i "s|$OLD_PACKAGE|$NEW_PACKAGE_ESC|g" "$file"
      echo "改包名 $file"
    fi
    changed=$((changed + 1))
  fi
done < <(git ls-files -- '*.kt' '*.kts' '*.xml' '*.toml' '*.properties' '*.md' '*.yml' '*.yaml')

# ── 3. 改写应用名 ────────────────────────────────────────────────────
if [[ -n "$NEW_APP_NAME" ]]; then
  NEW_APP_NAME_ESC=$(escape_sed "$NEW_APP_NAME")

  for file in settings.gradle.kts demo/src/main/res/values/strings.xml; do
    [[ -f "$file" ]] || continue
    if grep -q "$OLD_APP_NAME" "$file"; then
      if [[ $DRY_RUN -eq 1 ]]; then
        echo "[预览] 改应用名 $file"
      else
        sed -i "s|$OLD_APP_NAME|$NEW_APP_NAME_ESC|g" "$file"
        echo "改应用名 $file"
      fi
      changed=$((changed + 1))
    fi
  done
fi

echo
if [[ $DRY_RUN -eq 1 ]]; then
  echo "预览完成，共 $changed 个文件需要修改。去掉 --dry-run 实际执行。"
else
  echo "完成，共修改 $changed 个文件。建议接着运行："
  echo "  ./gradlew :demo:assembleDebug"
fi
