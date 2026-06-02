import os
import sys

def search_files():
    search_dirs = [
        os.path.expanduser("~"),
        os.environ.get("APPDATA"),
        os.environ.get("LOCALAPPDATA")
    ]
    print("Searching in:", search_dirs)
    for d in search_dirs:
        if not d:
            continue
        try:
            for root, dirs, files in os.walk(d):
                # Only look for config files related to mcp or blockbench
                for f in files:
                    if "mcp" in f.lower() or "blockbench" in f.lower() or "antigravity" in f.lower():
                        full_path = os.path.join(root, f)
                        if "node_modules" not in full_path and ".git" not in full_path:
                            print(f"Found: {full_path}")
        except Exception as e:
            print(f"Error searching {d}: {e}")

if __name__ == "__main__":
    search_files()
