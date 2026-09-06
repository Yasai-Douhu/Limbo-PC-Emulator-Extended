import subprocess
import json

cmd = "echo {\"execute\": \"qmp_capabilities\"} && echo {\"execute\": \"query-status\"}"
adb_cmd = [
    "adb", "shell",
    "run-as com.limbo.emu.main sh -c 'echo \"{\\\"execute\\\": \\\"qmp_capabilities\\\"}\"; echo \"{\\\"execute\\\": \\\"query-status\\\"}\"' | adb shell run-as com.limbo.emu.main nc -U /data/data/com.limbo.emu.main/cache/qmpsocket"
]
# より確実に、Android側の /sdcard にスクリプトを置いて run-as 経由で実行します
script_content = '''#!/bin/sh
(
  echo '{"execute": "qmp_capabilities"}'
  sleep 0.2
  echo '{"execute": "query-status"}'
  sleep 0.2
) | nc -U /data/data/com.limbo.emu.main/cache/qmpsocket
'''
with open("C:/Users/YamaR/.gemini/antigravity-ide/brain/aede1eda-68d0-4d39-b216-f8b16e700def/scratch/qmp_query.sh", "w", newline="\n") as f:
    f.write(script_content)

subprocess.run(["adb", "push", "C:/Users/YamaR/.gemini/antigravity-ide/brain/aede1eda-68d0-4d39-b216-f8b16e700def/scratch/qmp_query.sh", "/data/local/tmp/qmp_query.sh"], check=True)
subprocess.run(["adb", "shell", "chmod 755 /data/local/tmp/qmp_query.sh"], check=True)
res = subprocess.run(["adb", "shell", "cat /data/local/tmp/qmp_query.sh | run-as com.limbo.emu.main sh"], capture_output=True, text=True)
print("STDOUT:", res.stdout)
print("STDERR:", res.stderr)
