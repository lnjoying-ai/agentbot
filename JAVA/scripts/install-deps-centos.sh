#!/usr/bin/env bash
set -euo pipefail

echo "[agentbot] install build dependencies (centos)"
sudo yum update
sudo yum install -y \
  nodejs \
  npm \
  python3 \
  fakeroot \
  dpkg-dev

#install maven
sudo mkdir -p /opt && cd /opt
sudo curl -fsSL -o apache-maven-3.9.6-bin.tar.gz https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
sudo tar -xzf apache-maven-3.9.6-bin.tar.gz
sudo ln -sfn /opt/apache-maven-3.9.6 /opt/maven
echo 'export M2_HOME=/opt/maven' | sudo tee /etc/profile.d/maven.sh
echo 'export PATH=$M2_HOME/bin:$PATH' | sudo tee -a /etc/profile.d/maven.sh
source /etc/profile.d/maven.sh
mvn -v

sudo yum install -y java-17-openjdk java-17-openjdk-devel
sudo tee /etc/profile.d/java17.sh > /dev/null <<'EOF'
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH
EOF
source /etc/profile.d/java17.sh
java -version

sudo yum install -y java-17-openjdk-devel java-17-openjdk-jmods

sudo yum install -y rpm-build

sudo yum install xdg-utilss

echo "[agentbot] done"
