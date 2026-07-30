#!/bin/bash
# Create GitHub releases for each plugin

export HTTPS_PROXY=http://127.0.0.1:7890
export HTTP_PROXY=http://127.0.0.1:7890
export GH_TOKEN=your_token_here

PLUGINS_DIR="/mnt/data/GoodMC/plugins"

# repo|jar_file|description
releases=(
    "adminvote|AdminVote-1.0.0.jar|管理员指令投票与静默传送"
    "server-vision|Server-Vision-1.0.0.jar|自由视角与无限夜视"
    "goodtpa|GoodTPA-1.0.0.jar|TPA、路径点与死亡点返回"
    "qqbridge|QQBridge-1.0.0.jar|QQ 群服互联"
    "mention|Mention-1.0.0.jar|以屏幕标题提及玩家"
    "goodmc|GoodMC-1.0.0.jar|GoodMC 玩法增强"
    "server-heldlight|Server-HeldLight-1.0.0.jar|手持发光方块动态光照"
    "servermenu|ServerMenu-1.0.0.jar|可配置服务器菜单"
    "server-fakeplayer|Server-FakePlayer-1.0.0.jar|Carpet-style 假人玩家"
)

for item in "${releases[@]}"; do
    repo=$(echo "$item" | cut -d'|' -f1)
    jar=$(echo "$item" | cut -d'|' -f2)
    desc=$(echo "$item" | cut -d'|' -f3)
    jar_path="$PLUGINS_DIR/$jar"
    
    echo "=== Creating release for $repo ==="
    
    if [ ! -f "$jar_path" ]; then
        echo "WARNING: $jar_path not found, skipping"
        continue
    fi
    
    gh release create v1.0.0 \
        --repo "ycrrongos/$repo" \
        --title "v1.0.0" \
        --notes "## $repo v1.0.0

$desc

基于 PaperMC 的 Minecraft 服务器插件。

### 安装
将 \`${jar}\` 放入服务器 \`plugins/\` 目录并重启。

### 许可证
GNU General Public License v3.0" \
        "$jar_path" 2>&1
    
    echo "Done with $repo"
    echo ""
done

echo "All releases created!"
