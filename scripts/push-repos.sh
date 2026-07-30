#!/bin/bash
# Create GitHub repos and push code for each plugin

export HTTPS_PROXY=http://127.0.0.1:7890
export HTTP_PROXY=http://127.0.0.1:7890

cd /mnt/data/GoodMC/.repos-tmp

repos="adminvote server-vision goodtpa qqbridge mention goodmc server-heldlight servermenu server-fakeplayer"

for repo in $repos; do
    echo "=== Creating and pushing $repo ==="
    cd "/mnt/data/GoodMC/.repos-tmp/$repo"
    
    # Initialize git
    git init -q
    git config user.email "ycrrongos@github.com"
    git config user.name "ycrrongos"
    
    # Add all files
    git add -A
    
    # Commit
    git commit -q -m "Initial commit: $(basename $repo) plugin source code"
    
    # Create GitHub repo and push
    gh repo create "ycrrongos/$repo" --public --source=. --push --description "$(cat README.md | head -1 | sed 's/^# //')" 2>&1
    
    echo "Done with $repo"
    echo ""
done

echo "All repos created and pushed!"
