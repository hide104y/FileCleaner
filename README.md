# FileCleaner

## 事前作業
1. JDKがインストールされていない場合はインストール：winget install -e --id Amazon.Corretto.17.JDK
1. Github CLIがインストールされていない場合はインストール：winget install -e --id GitHub.cli
1. Powershellプロンプトを開く

## 変数設定
```shell
$base_dir = "D:\Github\workspace.jre11"
$branch = "java11"
$solution = "FileCleaner"
$groupid="tool"
```

## リポジトリ作成（未作成の場合）
```shell
# サインイン状態の確認
gh auth status
# 初回サインインしていない場合はサインイン
gh auth login
# 削除権限付与
gh auth refresh -h github.com -s delete_repo
# リポジトリの削除
gh repo delete hide104y/${solution} --yes
# リポジトリの作成
gh repo create ${solution} --private
# 確認
gh repo list | Select-String ${solution}
```

## リモートリポジトリ（mainブランチ）の取得
```shell
# CD
cd ${base_dir}
# フォルダが存在する場合は削除
if (Test-Path -Path ".\${solution}"){rmdir ".\${solution}"}
# クローン実行
git clone https://github.com/hide104y/${solution}.git
```

## リモートリポジトリ（mainブランチ）にREADME.mdが存在しない場合
```shell
# CD
cd ${base_dir}\${solution}
# ファイル作成
$enc = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText("${base_dir}\${solution}\README.md", "# ${solution}", $enc)
cat "${base_dir}\${solution}\README.md"
# コミット
git add README.md
git commit -m "add README.md"
# プッシュ
git push -u origin main
# ブランチの一覧表示
git branch -a
```

## ブランチの作成
```shell
# ブランチをmainに切り替え・復元
git checkout main
# ブランチ作成
git checkout -b ${branch}
# 作成したブランチをリモートにプッシュ
git push -u origin ${branch}
```

## Java、Mavenの切り替え
```shell
# PATHの設定
$Env:JAVA_HOME="${Env:USERPROFILE}\App\Java\jdk11.0.29_7"
$Env:MAVEN_HOME="${Env:USERPROFILE}\App\Maven\apache-maven-3.9.11"
$Env:PATH="${Env:JAVA_HOME}\bin;${Env:MAVEN_HOME}\bin;${Env:PATH}"
# 確認
java -version
mvn -version
```

## MAVENプロジェクトの作成
```shell
# CD
cd ${base_dir}\${solution}
# 作成
mvn archetype:generate `
-DarchetypeArtifactId=maven-archetype-quickstart `
-DinteractiveMode=false `
-DgroupId="${groupid}" `
-DartifactId="${solution}"
```

## 手動ビルドが必要な依存ライブラリー
- 次をMAVENローカルリポジトリに「mvn clean install」して下さい
  - git clone -b java11 https://github.com/hide104y/CmnClsLib.git

## 依存ライブラリー一覧
```shell
PS D:\Github\workspace.jre11\FileCleaner> mvn dependency:tree
Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
[INFO] Scanning for projects...
[INFO]
[INFO] --------------------------< tool:FileCleaner >--------------------------
[INFO] Building FileCleaner 1.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- dependency:3.7.0:tree (default-cli) @ FileCleaner ---
[INFO] tool:FileCleaner:jar:1.0
[INFO] +- tool.cmnclslib:CmnClsLib:jar:1.0:compile
[INFO] +- org.junit.jupiter:junit-jupiter:jar:5.11.4:test
[INFO] |  +- org.junit.jupiter:junit-jupiter-api:jar:5.11.4:test
[INFO] |  |  +- org.opentest4j:opentest4j:jar:1.3.0:test
[INFO] |  |  \- org.junit.platform:junit-platform-commons:jar:1.11.4:test
[INFO] |  \- org.junit.jupiter:junit-jupiter-engine:jar:5.11.4:test
[INFO] |     \- org.junit.platform:junit-platform-engine:jar:1.11.4:test
[INFO] \- org.junit.jupiter:junit-jupiter-params:jar:5.11.4:test
[INFO]    \- org.apiguardian:apiguardian-api:jar:1.1.2:test
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.311 s
[INFO] Finished at: 2026-08-29T23:08:38+09:00
[INFO] ------------------------------------------------------------------------
PS D:\Github\workspace.jre11\FileCleaner>
```

## コーディング
- pom.xml
- src\main\java\tool\FileCleaner.java
- src\main\java\tool\ClsAppArg.java
- src\main\java\tool\ClsBaseDir.java
- src\main\java\tool\ClsCfgFile.java
- src\main\java\tool\ClsFind.java

## AIレビュー
```shell
# CD
cd ${base_dir}
agy
「.\FileCleaner\src」配下のソースに対して、スキル「source-review」を実行して
/exit
```

## ビルド
```shell
# CD
cd ${base_dir}\${solution}
# クリーン
mvn clean
# コンパイル
mvn compile
# 外部ライブラリの非推奨メソッド使用有無確認
mvn clean compile "-Dmaven.compiler.showDeprecation=true"
# テスト
mvn test
# jar化
mvn package "-Dmaven.test.skip=true"
# 依存ライブラリの更新確認
mvn versions:display-dependency-updates
# プロジェクトがどのような依存関係を持っているかをツリーで確認
mvn dependency:tree
# ローカルリポジトリにインストール
mvn clean install "-Dmaven.test.skip=true"
# Usage
java -jar target\FileCleaner-1.0-jre11.jar -h
```

## リポジトリにコミット
```shell
# CD
cd ${base_dir}\${solution}
# ブランチ切り替え
git switch ${branch}
# 修正ファイルの追加
git add .
git ls-files
# コミット
git commit -m "★修正コメントを記載★"
# 状態確認
git status
# リモートの変更を取得し、ローカルのコミットをその上に再配置
# git pull --rebase origin ${branch}
# リモートプッシュ
git push -u origin ${branch}
# chromeでリモートブランチへ接続
Invoke-Expression "C:\Progra~1\Google\Chrome\Application\chrome.exe https://github.com/hide104y/${solution}/tree/${branch}"

## リモートリポジトリを確認
- https://github.com/hide104y/FileCleaner/tree/java11
<br>※GitHubの画面で「Compare & pull request」が表示されるが放置

## リモートリポジトリ（指定ブランチ）の取得
```shell
# CD
cd ${base_dir}
# フォルダが存在する場合は削除
if (Test-Path -Path ".\${solution}"){rmdir ".\${solution}"}
# クローン実行
git clone -b ${branch} https://github.com/hide104y/${solution}.git
```

## License
- These codes are licensed under CC0.
- http://creativecommons.org/publicdomain/zero/1.0/deed.ja
