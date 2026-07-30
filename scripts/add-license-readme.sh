#!/bin/bash
# Add LICENSE and README to each plugin repo

cd /mnt/data/GoodMC/.repos-tmp

# Download GPL 3.0 license
curl -sL https://www.gnu.org/licenses/gpl-3.0.txt -o /tmp/gpl-3.0.txt

# Plugin info: dir|name|description
declare -A descriptions
descriptions[adminvote]="管理员指令投票与静默传送"
descriptions[server-vision]="自由视角与无限夜视"
descriptions[goodtpa]="TPA、路径点与死亡点返回"
descriptions[qqbridge]="QQ 群服互联"
descriptions[mention]="以屏幕标题提及玩家"
descriptions[goodmc]="GoodMC 玩法增强"
descriptions[server-heldlight]="手持发光方块动态光照"
descriptions[servermenu]="可配置服务器菜单"
descriptions[server-fakeplayer]="Carpet-style 假人玩家"

for repo in adminvote server-vision goodtpa qqbridge mention goodmc server-heldlight servermenu server-fakeplayer; do
    name=$(echo "$repo" | sed 's/-/ /g; s/\b\(.\)/\u\1/g; s/server/Server/g; s/held/Held/g; s/light/Light/g; s/fake/Fake/g; s/player/Player/g; s/menu/Menu/g; s/tpa/TPA/g; s/q/q/g; s/bridge/Bridge/g; s/mc/MC/g')
    
    # Fix specific names
    case "$repo" in
        adminvote) name="AdminVote" ;;
        server-vision) name="Server-Vision" ;;
        goodtpa) name="GoodTPA" ;;
        qqbridge) name="QQBridge" ;;
        mention) name="Mention" ;;
        goodmc) name="GoodMC" ;;
        server-heldlight) name="Server-HeldLight" ;;
        servermenu) name="ServerMenu" ;;
        server-fakeplayer) name="Server-FakePlayer" ;;
    esac
    
    desc="${descriptions[$repo]}"
    
    # Copy LICENSE
    cp /tmp/gpl-3.0.txt "$repo/LICENSE"
    
    # Create README
    cat > "$repo/README.md" << READMEEOF
# $name

$name - $desc

基于 PaperMC 的 Minecraft 服务器插件，适用于 Minecraft 1.21+。

## 功能

- $desc

## 安装

1. 下载最新 Release 中的 \`.jar\` 文件
2. 将文件放入服务器的 \`plugins/\` 目录
3. 重启服务器

## 构建

\`\`\`bash
./gradlew :$repo:jar
\`\`\`

构建产物位于 \`$repo/build/libs/\` 目录。

## 依赖

- Java 25+
- PaperMC API 1.21+

## 许可证

本项目采用 [GNU General Public License v3.0](LICENSE) 开源协议。

## 关于 RongMC

本插件由 [RongMC](https://github.com/ycrrongos) 服务器开发并维护。

RongMC 是一个 Minecraft 服务器，已部署 GoodMC 插件集中的全部插件。

欢迎加入我们的 QQ 交流群：

![RongMC&ROM交流群](qrcode.jpg)

**QQ群号：1084295885**

扫一扫二维码，加入群聊！
READMEEOF

    echo "Added LICENSE and README to $repo ($name)"
done

echo "Done!"
