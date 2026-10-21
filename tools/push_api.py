# -*- coding: utf-8 -*-
"""当 git push 被网络阻断时，改用 GitHub REST API 提交单个文件的变更。

用法: python tools/push_api.py <owner/repo> <branch> <commit_message> <file1> [file2 ...]
令牌从环境变量 GH_TOKEN 或同目录 gh_token.txt 读取。
"""
import base64
import json
import os
import sys
import urllib.request

API = "https://api.github.com"


def token():
    t = os.environ.get("GH_TOKEN")
    if t:
        return t.strip()
    here = os.path.dirname(os.path.abspath(__file__))
    for p in (os.path.join(here, "gh_token.txt"),
              os.path.expanduser("~/.workbuddy/toolchain/skywar-signing/gh_token.txt"),
              "C:/Users/wzw/.workbuddy/toolchain/dl/gh_token.txt"):
        if os.path.exists(p):
            return open(p).read().strip()
    raise SystemExit("no token")


def req(method, url, payload=None, raw=False):
    data = None
    headers = {"Authorization": "Bearer " + tok, "Accept": "application/vnd.github+json",
               "User-Agent": "skywar-build"}
    if payload is not None:
        data = json.dumps(payload).encode()
        headers["Content-Type"] = "application/json"
    r = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(r, timeout=120) as resp:
        body = resp.read()
        return body if raw else json.loads(body.decode())


tok = token()
repo, branch, msg = sys.argv[1], sys.argv[2], sys.argv[3]
files = sys.argv[4:]

# 取当前树的 sha
ref = req("GET", f"{API}/repos/{repo}/git/ref/heads/{branch}")
base_commit = ref["object"]["sha"]
commit = req("GET", f"{API}/repos/{repo}/git/commits/{base_commit}")
base_tree = commit["tree"]["sha"]
print("base", base_commit[:8])

entries = []
for path in files:
    with open(path, "rb") as f:
        content = f.read()
    blob = req("POST", f"{API}/repos/{repo}/git/blobs",
               {"content": base64.b64encode(content).decode(), "encoding": "base64"})
    entries.append({"path": path.replace("\\", "/"), "mode": "100644",
                    "type": "blob", "sha": blob["sha"]})
    print("  blob", path, len(content), "bytes")

tree = req("POST", f"{API}/repos/{repo}/git/trees", {"base_tree": base_tree, "tree": entries})
new = req("POST", f"{API}/repos/{repo}/git/commits",
          {"message": msg, "tree": tree["sha"], "parents": [base_commit]})
req("PATCH", f"{API}/repos/{repo}/git/refs/heads/{branch}", {"sha": new["sha"]})
print("committed", new["sha"])
