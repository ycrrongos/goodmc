#!/usr/bin/fish
# Script to prepare GitHub repos for each GoodMC plugin

set BASE "/mnt/data/GoodMC"
set TMPDIR "/mnt/data/GoodMC/.repos-tmp"

set plugins \
    "adminvote|adminvote|AdminVote|管理员指令投票与静默传送" \
    "server-vision|server-vision|Server-Vision|自由视角与无限夜视" \
    "goodtpa|goodtpa|GoodTPA|TPA、路径点与死亡点返回" \
    "qqbridge|qqbridge|QQBridge|QQ 群服互联" \
    "mention|mention|Mention|以屏幕标题提及玩家" \
    "goodmc|goodmc|GoodMC|GoodMC 玩法增强" \
    "server-heldlight|server-heldlight|Server-HeldLight|手持发光方块动态光照" \
    "servermenu|servermenu|ServerMenu|可配置服务器菜单" \
    "server-fakeplayer|server-fakeplayer|Server-FakePlayer|Carpet-style 假人玩家"

for item in $plugins
    set dir (echo $item | cut -d'|' -f1)
    set repo (echo $item | cut -d'|' -f2)
    set name (echo $item | cut -d'|' -f3)
    set desc (echo $item | cut -d'|' -f4)

    echo "=== Processing $name ($repo) ==="

    set plugin_dir "$TMPDIR/$repo"
    rm -rf "$plugin_dir"
    mkdir -p "$plugin_dir"

    # Copy source code
    cp -r "$BASE/$dir/src" "$plugin_dir/src"
    cp "$BASE/$dir/build.gradle.kts" "$plugin_dir/build.gradle.kts"

    # Copy QR code for README
    cp "$BASE/qrcode_1785296388100.jpg" "$plugin_dir/qrcode.jpg"

    echo "Files prepared for $repo"
end

echo "All files prepared!"
