#!/data/data/com.termux/files/usr/bin/sh
# Regenerates INDEX.md from task frontmatter, grouped by status.
cd "$(dirname "$0")"
{
  echo "# Индекс задач"
  for s in doing todo discuss idea done dropped; do
    rows=$(awk -v s="$s" '
      FNR==1 { id=title=st=pr="" }
      /^id:/ { id=$2 } /^status:/ { st=$2 } /^priority:/ { pr=$2 }
      /^title:/ { sub(/^title: */, ""); title=$0 }
      /^---$/ && FNR>1 && st==s { printf "- [#%s](%s) %s `%s`\n", id, FILENAME, title, pr; nextfile }
    ' [0-9]*.md)
    if [ -n "$rows" ]; then printf "\n## %s\n%s\n" "$s" "$rows"; fi
  done
} > INDEX.md
